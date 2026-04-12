import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import LoginPage from "@/app/(auth)/login/page";

// ========================================
// 로그인 페이지 통합 테스트
// ========================================

// Next.js router/navigation mock
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

// useAuth mock
const mockLogin = vi.fn();

vi.mock("@/hooks/use-auth", () => ({
  useAuth: () => ({
    login: mockLogin,
    isLoading: false,
    error: null,
    isAuthenticated: false,
    user: null,
  }),
}));

describe("LoginPage 통합 테스트", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLogin.mockResolvedValue({ success: false, error: "로그인에 실패했습니다." });
  });

  it("로그인 페이지 제목이 렌더링되어야 한다", () => {
    render(<LoginPage />);

    // CardTitle은 div로 렌더링되므로 data-slot 속성으로 조회
    const cardTitle = document.querySelector('[data-slot="card-title"]');
    expect(cardTitle).toHaveTextContent("로그인");
    expect(screen.getByText("Rental Commerce에 로그인하세요")).toBeInTheDocument();
  });

  it("이메일, 비밀번호 입력 필드가 렌더링되어야 한다", () => {
    render(<LoginPage />);

    expect(screen.getByLabelText("이메일")).toBeInTheDocument();
    expect(screen.getByLabelText("비밀번호")).toBeInTheDocument();
  });

  it("소셜 로그인 버튼이 렌더링되어야 한다", () => {
    render(<LoginPage />);

    expect(screen.getByRole("button", { name: "카카오로 로그인" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "네이버로 로그인" })).toBeInTheDocument();
  });

  it("회원가입 링크가 있어야 한다", () => {
    render(<LoginPage />);

    const signupLink = screen.getByRole("link", { name: "회원가입" });
    expect(signupLink).toBeInTheDocument();
    expect(signupLink).toHaveAttribute("href", "/signup");
  });

  it("올바른 자격증명으로 로그인하면 홈으로 리다이렉트되어야 한다", async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue({ success: true });
    render(<LoginPage />);

    await user.type(screen.getByLabelText("이메일"), "lender@rental.com");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/");
    });
  });

  it("login이 실패하면 홈으로 리다이렉트되지 않아야 한다", async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue({ success: false, error: "이메일 또는 비밀번호가 올바르지 않습니다." });
    render(<LoginPage />);

    await user.type(screen.getByLabelText("이메일"), "wrong@test.com");
    await user.type(screen.getByLabelText("비밀번호"), "wrongpassword");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        email: "wrong@test.com",
        password: "wrongpassword",
      });
    });
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("이메일을 입력하지 않고 제출하면 유효성 오류가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(screen.getByText("이메일을 입력해주세요.")).toBeInTheDocument();
    });
    expect(mockLogin).not.toHaveBeenCalled();
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("로그인 버튼 클릭 시 login 함수가 올바른 인자로 호출되어야 한다", async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue({ success: true });
    render(<LoginPage />);

    await user.type(screen.getByLabelText("이메일"), "test@test.com");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        email: "test@test.com",
        password: "password123",
      });
    });
  });
});
