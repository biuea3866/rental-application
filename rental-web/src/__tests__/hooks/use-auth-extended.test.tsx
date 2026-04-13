import { describe, it, expect, beforeEach, vi } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useAuth } from "@/hooks/use-auth";
import { useAuthStore } from "@/stores/auth-store";
import { clearTokens, setAccessToken } from "@/lib/auth/token";

// ========================================
// useAuth 훅 확장 테스트 (실제 API 호출 경로)
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/",
}));

describe("useAuth 확장 테스트", () => {
  beforeEach(() => {
    const store = useAuthStore.getState();
    store.logout();
    useAuthStore.setState({ isLoading: false, error: null });
    clearTokens();
    vi.clearAllMocks();
  });

  describe("login 실제 API 호출", () => {
    it("올바른 자격증명으로 로그인 성공 시 인증 상태가 true여야 한다", async () => {
      const { result } = renderHook(() => useAuth());

      let loginResult: { success: boolean; error?: string } | undefined;

      await act(async () => {
        loginResult = await result.current.login({
          email: "lender@rental.com",
          password: "password123",
        });
      });

      expect(loginResult?.success).toBe(true);
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user).not.toBeNull();
    });

    it("잘못된 비밀번호로 로그인 실패 시 error가 설정되어야 한다", async () => {
      const { result } = renderHook(() => useAuth());

      let loginResult: { success: boolean; error?: string } | undefined;

      await act(async () => {
        loginResult = await result.current.login({
          email: "lender@rental.com",
          password: "wrong_password",
        });
      });

      expect(loginResult?.success).toBe(false);
      expect(loginResult?.error).toBeDefined();
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe("signup 실제 API 호출", () => {
    it("신규 이메일로 회원가입 성공 시 결과를 반환해야 한다", async () => {
      const { result } = renderHook(() => useAuth());

      let signupResult: { success: boolean; error?: string } | undefined;

      await act(async () => {
        signupResult = await result.current.signup({
          email: "newuser123@rental.com",
          password: "password123",
          name: "새사용자",
          phone: "010-9876-5432",
          role: "RENTER",
        });
      });

      expect(signupResult?.success).toBe(true);
    });

    it("중복 이메일로 회원가입 실패 시 error가 설정되어야 한다", async () => {
      const { result } = renderHook(() => useAuth());

      let signupResult: { success: boolean; error?: string } | undefined;

      await act(async () => {
        signupResult = await result.current.signup({
          email: "lender@rental.com", // 이미 존재하는 이메일
          password: "password123",
          name: "중복사용자",
          phone: "010-0000-0000",
          role: "RENTER",
        });
      });

      expect(signupResult?.success).toBe(false);
      expect(signupResult?.error).toBeDefined();
    });
  });

  describe("logout 실제 API 호출", () => {
    it("로그아웃 시 인증 상태가 초기화되어야 한다", async () => {
      // 먼저 로그인 상태 설정
      useAuthStore.setState({
        user: {
          id: "user-001",
          email: "test@test.com",
          name: "테스트",
          phone: "010-0000-0000",
          role: "RENTER",
          createdAt: "2026-01-01T00:00:00Z",
        },
        isAuthenticated: true,
      });
      setAccessToken("test-token", 3600);

      const { result } = renderHook(() => useAuth());
      expect(result.current.isAuthenticated).toBe(true);

      await act(async () => {
        await result.current.logout();
      });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
    });

    it("로그아웃 API 실패해도 로컬 상태는 초기화되어야 한다", async () => {
      const { http, HttpResponse } = await import("msw");
      const { server } = await import("@/mocks/server");

      server.use(
        http.post("http://localhost:8080/api/v1/auth/logout", () => {
          return HttpResponse.json(
            { code: "SERVER_ERROR", message: "서버 오류" },
            { status: 500 }
          );
        })
      );

      useAuthStore.setState({
        user: {
          id: "user-001",
          email: "test@test.com",
          name: "테스트",
          phone: "010-0000-0000",
          role: "RENTER",
          createdAt: "2026-01-01T00:00:00Z",
        },
        isAuthenticated: true,
      });

      const { result } = renderHook(() => useAuth());

      await act(async () => {
        await result.current.logout();
      });

      // API 실패해도 로컬 상태는 초기화
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
    });
  });

  describe("fetchUser", () => {
    it("fetchUser 호출 시 유저 정보가 스토어에 저장되어야 한다", async () => {
      setAccessToken("test-access-token", 3600);

      const { result } = renderHook(() => useAuth());

      await act(async () => {
        await result.current.fetchUser();
      });

      expect(result.current.user).not.toBeNull();
    });

    it("fetchUser API 실패 시 에러가 무시되어야 한다", async () => {
      const { http, HttpResponse } = await import("msw");
      const { server } = await import("@/mocks/server");

      server.use(
        http.get("http://localhost:8080/api/v1/auth/me", () => {
          return HttpResponse.json(
            { code: "UNAUTHORIZED", message: "인증 필요" },
            { status: 401 }
          );
        })
      );

      const { result } = renderHook(() => useAuth());

      await act(async () => {
        // 에러가 발생해도 예외가 던져지지 않아야 한다
        await expect(result.current.fetchUser()).resolves.not.toThrow();
      });
    });
  });

  describe("checkAuth", () => {
    it("checkAuth 호출 시 에러가 발생하지 않아야 한다", async () => {
      const { result } = renderHook(() => useAuth());

      await act(async () => {
        await expect(result.current.checkAuth()).resolves.not.toThrow();
      });
    });
  });
});
