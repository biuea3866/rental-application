import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import LoginPage from "@/app/(auth)/login/page";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// 로그인 페이지 추가 테스트 (소셜 로그인, 기타 role)
// ========================================

const mockPush = vi.fn();
const mockReplace = vi.fn();

Object.defineProperty(window, "location", {
  writable: true,
  value: { ...window.location, href: "", origin: "http://localhost:3000" },
});

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/login",
  useSearchParams: () => new URLSearchParams(),
}));

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

describe("로그인 페이지 추가 테스트", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  });

  it("역할이 없는 경우 / 로 이동해야 한다", async () => {
    const user = userEvent.setup();

    mockLoginFn.mockImplementation(async () => {
      useAuthStore.setState({
        user: {
          id: "user-001",
          email: "unknown@rental.com",
          name: "알수없음",
          phone: "010-0000-0000",
          role: "RENTER", // 사실 role이 없는 케이스는 없지만 테스트용
          createdAt: "2026-01-01T00:00:00Z",
        },
        isAuthenticated: true,
      });
      // user를 null로 만들어서 else 분기 테스트
      useAuthStore.setState({ user: null });
      return { success: true };
    });

    render(<LoginPage />);

    await user.type(screen.getByLabelText("이메일"), "unknown@rental.com");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/");
    });
  });

  it("소셜 로그인 버튼들이 렌더링되어야 한다", () => {
    render(<LoginPage />);

    expect(
      screen.getByRole("button", { name: /카카오로 로그인/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /네이버로 로그인/i })
    ).toBeInTheDocument();
  });

  it("소셜 로그인 버튼 클릭 시 window.location.href가 변경되어야 한다", async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole("button", { name: /카카오로 로그인/i }));

    // window.location.href가 카카오 URL로 변경되어야 한다
    expect(window.location.href).toContain("kakao");
  });

  it("'계정이 없으신가요?' 회원가입 링크가 있어야 한다", () => {
    render(<LoginPage />);

    expect(screen.getByText(/계정이 없으신가요\?/)).toBeInTheDocument();
    const signupLink = screen.getByRole("link", { name: "회원가입" });
    expect(signupLink).toHaveAttribute("href", "/signup");
  });
});
