import { describe, it, expect, beforeEach, vi } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useAuth } from "@/hooks/use-auth";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// useAuth 훅 테스트
// ========================================

// Next.js router mock
vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/",
}));

describe("useAuth hook", () => {
  beforeEach(() => {
    // 매 테스트마다 스토어 초기화
    const store = useAuthStore.getState();
    store.logout();
    useAuthStore.setState({ isLoading: false });
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("초기 상태를 올바르게 반환해야 한다", () => {
    const { result } = renderHook(() => useAuth());

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it("login, signup, logout, fetchUser, checkAuth 함수가 존재해야 한다", () => {
    const { result } = renderHook(() => useAuth());

    expect(typeof result.current.login).toBe("function");
    expect(typeof result.current.signup).toBe("function");
    expect(typeof result.current.logout).toBe("function");
    expect(typeof result.current.fetchUser).toBe("function");
    expect(typeof result.current.checkAuth).toBe("function");
  });

  it("isLoading 상태를 올바르게 반환해야 한다", () => {
    useAuthStore.setState({ isLoading: true });

    const { result } = renderHook(() => useAuth());

    expect(result.current.isLoading).toBe(true);
  });

  it("error 상태를 올바르게 반환해야 한다", () => {
    useAuthStore.setState({ error: "테스트 에러" });

    const { result } = renderHook(() => useAuth());

    expect(result.current.error).toBe("테스트 에러");
  });

  it("스토어에 유저가 설정되면 isAuthenticated가 true여야 한다", () => {
    useAuthStore.setState({
      user: {
        id: 1,
        email: "test@test.com",
        name: "테스트",
        phone: "010-0000-0000",
        role: "RENTER",
        createdAt: "2026-01-01T00:00:00Z",
      },
      isAuthenticated: true,
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user?.email).toBe("test@test.com");
  });

  it("logout을 호출하면 인증 상태가 초기화되어야 한다", async () => {
    // 먼저 인증 상태 설정
    useAuthStore.setState({
      user: {
        id: 1,
        email: "test@test.com",
        name: "테스트",
        phone: "010-0000-0000",
        role: "RENTER",
        createdAt: "2026-01-01T00:00:00Z",
      },
      isAuthenticated: true,
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.isAuthenticated).toBe(true);

    await act(async () => {
      await result.current.logout();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
  });
});
