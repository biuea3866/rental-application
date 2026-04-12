import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { SignupForm } from "@/components/auth/SignupForm";

// ========================================
// SignupForm 테스트
// ========================================

describe("SignupForm", () => {
  const mockOnSubmit = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const fillValidForm = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.type(screen.getByLabelText("이메일"), "test@test.com");
    await user.type(screen.getByLabelText("이름"), "홍길동");
    await user.type(screen.getByLabelText("전화번호"), "010-1234-5678");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.type(screen.getByLabelText("비밀번호 확인"), "password123");
  };

  it("모든 입력 필드가 렌더링되어야 한다", () => {
    render(<SignupForm onSubmit={mockOnSubmit} />);

    expect(screen.getByLabelText("이메일")).toBeInTheDocument();
    expect(screen.getByLabelText("이름")).toBeInTheDocument();
    expect(screen.getByLabelText("전화번호")).toBeInTheDocument();
    expect(screen.getByLabelText("비밀번호")).toBeInTheDocument();
    expect(screen.getByLabelText("비밀번호 확인")).toBeInTheDocument();
    expect(screen.getByLabelText("역할 선택")).toBeInTheDocument();
  });

  it("회원가입 버튼이 렌더링되어야 한다", () => {
    render(<SignupForm onSubmit={mockOnSubmit} />);

    expect(
      screen.getByRole("button", { name: "회원가입" })
    ).toBeInTheDocument();
  });

  it("이름이 2자 미만이면 유효성 오류가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText("이름"), "홍");
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => {
      expect(
        screen.getByText("이름은 2자 이상이어야 합니다.")
      ).toBeInTheDocument();
    });
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it("올바르지 않은 전화번호 형식이면 유효성 오류가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText("전화번호"), "01012345678");
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => {
      expect(
        screen.getByText(
          "올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678)"
        )
      ).toBeInTheDocument();
    });
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it("비밀번호가 일치하지 않으면 유효성 오류가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText("이메일"), "test@test.com");
    await user.type(screen.getByLabelText("이름"), "홍길동");
    await user.type(screen.getByLabelText("전화번호"), "010-1234-5678");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.type(screen.getByLabelText("비밀번호 확인"), "different123");
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => {
      expect(
        screen.getByText("비밀번호가 일치하지 않습니다.")
      ).toBeInTheDocument();
    });
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it("올바른 값을 입력하면 onSubmit이 호출되어야 한다", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={mockOnSubmit} />);

    await fillValidForm(user);
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          email: "test@test.com",
          name: "홍길동",
          phone: "010-1234-5678",
          password: "password123",
          passwordConfirm: "password123",
          role: "RENTER",
        }),
        expect.anything()
      );
    });
  });

  it("서버 에러가 있으면 에러 메시지를 표시해야 한다", () => {
    render(
      <SignupForm
        onSubmit={mockOnSubmit}
        serverError="이미 등록된 이메일입니다."
      />
    );

    expect(screen.getByText("이미 등록된 이메일입니다.")).toBeInTheDocument();
  });

  it("isLoading이 true이면 버튼이 비활성화되고 '가입 중...' 텍스트가 표시되어야 한다", () => {
    render(<SignupForm onSubmit={mockOnSubmit} isLoading={true} />);

    const button = screen.getByRole("button", { name: /가입/ });
    expect(button).toBeDisabled();
    expect(screen.getByText("가입 중...")).toBeInTheDocument();
  });

  it("역할을 선택할 수 있어야 한다", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={mockOnSubmit} />);

    const roleSelect = screen.getByLabelText("역할 선택");
    await user.selectOptions(roleSelect, "LENDER");

    await fillValidForm(user);
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({ role: "LENDER" }),
        expect.anything()
      );
    });
  });
});
