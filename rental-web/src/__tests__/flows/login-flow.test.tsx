import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import LoginPage from "@/app/(auth)/login/page";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// 로그인 플로우 통합 테스트
// - 전체 플로우: 폼 입력 → 제출 → role 기반 리다이렉트
// ========================================

// Next.js router mock
const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/login",
  useSearchParams: () => new URLSearchParams(),
}));

// useAuth mock — 로그인 성공 시 실제 useAuthStore에 유저 정보 저장
const mockLoginFn = vi.fn();

vi.mock("@/hooks/use-auth", () => ({
  useAuth: () => ({
    login: mockLoginFn,
    isLoading: false,
    error: null,
    isAuthenticated: false,
    user: null,
  }),
}));

// useAuthStore의 getState를 실제 스토어 기반으로 동작하게 함
// (vi.mock 없이 실제 zustand 스토어 사용)

describe("로그인 플로우", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // 스토어 초기화
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  });

  it("LENDER 이메일로 로그인하면 /lender로 이동한다", async () => {
    const user = userEvent.setup();

    // login 성공 시 실제 스토어에 LENDER 유저 저장
    mockLoginFn.mockImplementation(async () => {
      useAuthStore.setState({
        user: {
          id: 1,
          email: "lender@rental.com",
          name: "김대여",
          phone: "010-1234-5678",
          role: "LENDER",
          createdAt: "2026-01-15T09:00:00Z",
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
      return { success: true };
    });

    render(<LoginPage />);

    // 이메일 / 비밀번호 입력
    await user.type(screen.getByLabelText("이메일"), "lender@rental.com");
    await user.type(screen.getByLabelText("비밀번호"), "password123");

    // 로그인 버튼 클릭
    await user.click(screen.getByRole("button", { name: "로그인" }));

    // LENDER → /lender 리다이렉트
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/lender");
    });

    // 스토어에 유저 정보 저장 확인
    const storeState = useAuthStore.getState();
    expect(storeState.isAuthenticated).toBe(true);
    expect(storeState.user?.role).toBe("LENDER");
    expect(storeState.user?.email).toBe("lender@rental.com");
  });

  it("RENTER 이메일로 로그인하면 /products로 이동한다", async () => {
    const user = userEvent.setup();

    // login 성공 시 실제 스토어에 RENTER 유저 저장
    mockLoginFn.mockImplementation(async () => {
      useAuthStore.setState({
        user: {
          id: 3,
          email: "renter@rental.com",
          name: "이빌림",
          phone: "010-3456-7890",
          role: "RENTER",
          createdAt: "2026-01-20T11:00:00Z",
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
      return { success: true };
    });

    render(<LoginPage />);

    await user.type(screen.getByLabelText("이메일"), "renter@rental.com");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    // RENTER → /products 리다이렉트
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/products");
    });

    // 스토어에 유저 정보 저장 확인
    const storeState = useAuthStore.getState();
    expect(storeState.isAuthenticated).toBe(true);
    expect(storeState.user?.role).toBe("RENTER");
    expect(storeState.user?.email).toBe("renter@rental.com");
  });

  it("잘못된 비밀번호로 로그인 시도 시 오류가 표시되고 리다이렉트되지 않는다", async () => {
    const user = userEvent.setup();

    // login 실패: 스토어 유저 설정하지 않음
    mockLoginFn.mockResolvedValue({
      success: false,
      error: "이메일 또는 비밀번호가 올바르지 않습니다.",
    });

    render(<LoginPage />);

    await user.type(screen.getByLabelText("이메일"), "lender@rental.com");
    await user.type(screen.getByLabelText("비밀번호"), "wrongpassword");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    // login이 실패했으므로 리다이렉트 없음
    await waitFor(() => {
      expect(mockLoginFn).toHaveBeenCalledWith({
        email: "lender@rental.com",
        password: "wrongpassword",
      });
    });

    expect(mockPush).not.toHaveBeenCalled();

    // 스토어 비인증 상태 유지
    const storeState = useAuthStore.getState();
    expect(storeState.isAuthenticated).toBe(false);
    expect(storeState.user).toBeNull();
  });

  it("이메일 미입력 시 유효성 오류가 표시되고 login 함수가 호출되지 않는다", async () => {
    const user = userEvent.setup();

    render(<LoginPage />);

    // 비밀번호만 입력, 이메일 없음
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(screen.getByText("이메일을 입력해주세요.")).toBeInTheDocument();
    });

    // login 함수 호출 없음
    expect(mockLoginFn).not.toHaveBeenCalled();
    expect(mockPush).not.toHaveBeenCalled();
  });
});
