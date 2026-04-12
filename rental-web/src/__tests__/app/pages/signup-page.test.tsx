import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import SignupPage from "@/app/(auth)/signup/page";

// ========================================
// 회원가입 페이지 테스트
// ========================================

const mockPush = vi.fn();
const mockReplace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/signup",
  useSearchParams: () => new URLSearchParams(),
}));

describe("회원가입 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("회원가입 제목이 렌더링되어야 한다", () => {
    render(<SignupPage />);
    // 회원가입 텍스트가 하나 이상 있어야 한다
    const elements = screen.getAllByText("회원가입");
    expect(elements.length).toBeGreaterThan(0);
  });

  it("'Rental Commerce에 가입하세요' 설명이 렌더링되어야 한다", () => {
    render(<SignupPage />);
    expect(screen.getByText("Rental Commerce에 가입하세요")).toBeInTheDocument();
  });

  it("이메일 입력 필드가 렌더링되어야 한다", () => {
    render(<SignupPage />);
    expect(screen.getByLabelText("이메일")).toBeInTheDocument();
  });

  it("비밀번호 입력 필드가 렌더링되어야 한다", () => {
    render(<SignupPage />);
    expect(screen.getByLabelText("비밀번호")).toBeInTheDocument();
  });

  it("'이미 계정이 있으신가요?' 텍스트가 있어야 한다", () => {
    render(<SignupPage />);
    expect(screen.getByText(/이미 계정이 있으신가요/)).toBeInTheDocument();
  });

  it("'로그인' 링크가 /login으로 연결되어야 한다", () => {
    render(<SignupPage />);
    const loginLink = screen.getByRole("link", { name: "로그인" });
    expect(loginLink).toHaveAttribute("href", "/login");
  });

  it("올바른 정보로 회원가입 성공 시 /signup/verify로 이동해야 한다", async () => {
    const user = userEvent.setup();

    render(<SignupPage />);

    await user.type(screen.getByLabelText("이메일"), "newuser@rental.com");
    await user.type(screen.getByLabelText("이름"), "새사용자");
    await user.type(screen.getByLabelText("전화번호"), "010-9999-0000");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.type(screen.getByLabelText("비밀번호 확인"), "password123");
    // 역할 선택 - select에서 RENTER 선택
    await user.selectOptions(screen.getByLabelText("역할 선택"), "RENTER");

    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(
      () => {
        expect(mockPush).toHaveBeenCalledWith(
          expect.stringContaining("/signup/verify")
        );
      },
      { timeout: 3000 }
    );
  });

  it("중복 이메일로 회원가입 시 서버 에러 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();

    render(<SignupPage />);

    await user.type(
      screen.getByLabelText("이메일"),
      "lender@rental.com" // 이미 존재하는 이메일
    );
    await user.type(screen.getByLabelText("이름"), "김대여2");
    await user.type(screen.getByLabelText("전화번호"), "010-1111-2222");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.type(screen.getByLabelText("비밀번호 확인"), "password123");
    await user.selectOptions(screen.getByLabelText("역할 선택"), "LENDER");

    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(
      () => {
        expect(screen.getByText(/이미 등록된 이메일/)).toBeInTheDocument();
      },
      { timeout: 3000 }
    );

    expect(mockPush).not.toHaveBeenCalled();
  });
});
