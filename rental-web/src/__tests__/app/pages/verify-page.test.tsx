import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import VerifyPage from "@/app/(auth)/signup/verify/page";
import { useAuthStore } from "@/stores/auth-store";
import { clearTokens } from "@/lib/auth/token";

// ========================================
// 인증코드 입력 페이지 테스트
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
  usePathname: () => "/signup/verify",
  useSearchParams: () => new URLSearchParams({ phone: "010-9999-0000" }),
}));

describe("인증코드 입력 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearTokens();
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  });

  it("'휴대폰 인증' 제목이 렌더링되어야 한다", async () => {
    render(<VerifyPage />);

    await waitFor(() => {
      expect(screen.getByText("휴대폰 인증")).toBeInTheDocument();
    });
  });

  it("인증코드 발송 설명 메시지가 표시되어야 한다", async () => {
    render(<VerifyPage />);

    await waitFor(() => {
      expect(
        screen.getByText("입력하신 번호로 인증코드를 발송했습니다.")
      ).toBeInTheDocument();
    });
  });

  it("전화번호가 표시되어야 한다", async () => {
    render(<VerifyPage />);

    await waitFor(() => {
      expect(screen.getByText(/010-9999-0000/)).toBeInTheDocument();
    });
  });

  it("'회원가입으로 돌아가기' 링크가 있어야 한다", async () => {
    render(<VerifyPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("link", { name: "회원가입으로 돌아가기" })
      ).toBeInTheDocument();
    });
  });

  it("인증코드 입력 필드가 렌더링되어야 한다", async () => {
    render(<VerifyPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("인증코드")).toBeInTheDocument();
    });
  });

  it("올바른 인증코드 입력 시 홈으로 이동해야 한다", async () => {
    const user = userEvent.setup();
    render(<VerifyPage />);

    await waitFor(() => {
      expect(screen.getByText("휴대폰 인증")).toBeInTheDocument();
    });

    const codeInput = screen.getByLabelText("인증코드");
    await user.type(codeInput, "123456");
    await user.click(screen.getByRole("button", { name: "인증 확인" }));

    await waitFor(
      () => {
        expect(mockPush).toHaveBeenCalledWith("/");
      },
      { timeout: 3000 }
    );
  });

  it("잘못된 인증코드 입력 시 에러 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<VerifyPage />);

    await waitFor(() => {
      expect(screen.getByText("휴대폰 인증")).toBeInTheDocument();
    });

    const codeInput = screen.getByLabelText("인증코드");
    await user.type(codeInput, "999999");
    await user.click(screen.getByRole("button", { name: "인증 확인" }));

    await waitFor(
      () => {
        expect(
          screen.getByText(/인증코드가 올바르지 않습니다/)
        ).toBeInTheDocument();
      },
      { timeout: 3000 }
    );

    expect(mockPush).not.toHaveBeenCalled();
  });

  it("재전송 버튼이 렌더링되어야 한다", async () => {
    render(<VerifyPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "인증코드 재전송" })
      ).toBeInTheDocument();
    });
  });

  it("재전송 버튼 클릭 시 인증코드 재전송 성공 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<VerifyPage />);

    await waitFor(() => {
      expect(screen.getByText("휴대폰 인증")).toBeInTheDocument();
    });

    // 재전송 버튼 클릭
    await user.click(screen.getByRole("button", { name: "인증코드 재전송" }));

    await waitFor(
      () => {
        expect(screen.getByText("인증코드가 재전송되었습니다.")).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });
});
