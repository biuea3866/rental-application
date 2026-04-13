import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { LoginForm } from "@/components/auth/LoginForm";

// ========================================
// LoginForm 테스트
// ========================================

describe("LoginForm", () => {
  const mockOnSubmit = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("이메일과 비밀번호 입력 필드가 렌더링되어야 한다", () => {
    render(<LoginForm onSubmit={mockOnSubmit} />);

    expect(screen.getByLabelText("이메일")).toBeInTheDocument();
    expect(screen.getByLabelText("비밀번호")).toBeInTheDocument();
  });

  it("로그인 버튼이 렌더링되어야 한다", () => {
    render(<LoginForm onSubmit={mockOnSubmit} />);

    expect(screen.getByRole("button", { name: "로그인" })).toBeInTheDocument();
  });

  it("이메일을 입력하지 않으면 유효성 오류가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<LoginForm onSubmit={mockOnSubmit} />);

    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(screen.getByText("이메일을 입력해주세요.")).toBeInTheDocument();
    });
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it("올바르지 않은 이메일 형식이면 유효성 오류가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<LoginForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText("이메일"), "invalid-email");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(
        screen.getByText("올바른 이메일 형식을 입력해주세요.")
      ).toBeInTheDocument();
    });
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it("비밀번호가 8자 미만이면 유효성 오류가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<LoginForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText("이메일"), "test@test.com");
    await user.type(screen.getByLabelText("비밀번호"), "short");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(
        screen.getByText("비밀번호는 8자 이상이어야 합니다.")
      ).toBeInTheDocument();
    });
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it("올바른 값을 입력하면 onSubmit이 호출되어야 한다", async () => {
    const user = userEvent.setup();
    render(<LoginForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText("이메일"), "test@test.com");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith(
        { email: "test@test.com", password: "password123" },
        expect.anything()
      );
    });
  });

  it("서버 에러가 있으면 에러 메시지를 표시해야 한다", () => {
    render(
      <LoginForm
        onSubmit={mockOnSubmit}
        serverError="이메일 또는 비밀번호가 올바르지 않습니다."
      />
    );

    expect(
      screen.getByText("이메일 또는 비밀번호가 올바르지 않습니다.")
    ).toBeInTheDocument();
  });

  it("isLoading이 true이면 버튼이 비활성화되고 '로그인 중...' 텍스트가 표시되어야 한다", () => {
    render(<LoginForm onSubmit={mockOnSubmit} isLoading={true} />);

    const button = screen.getByRole("button", { name: /로그인/ });
    expect(button).toBeDisabled();
    expect(screen.getByText("로그인 중...")).toBeInTheDocument();
  });
});
