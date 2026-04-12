import { render, screen, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { PhoneVerification } from "@/components/auth/PhoneVerification";

// ========================================
// PhoneVerification 테스트
// ========================================

const mockPhone = "010-1234-5678";

describe("PhoneVerification - 기본 렌더링", () => {
  it("전화번호와 인증코드 입력 필드가 렌더링되어야 한다", () => {
    const mockOnVerify = vi.fn().mockResolvedValue({ success: true });
    const mockOnResend = vi.fn().mockResolvedValue({ success: true });

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    expect(screen.getByText(mockPhone, { exact: false })).toBeInTheDocument();
    expect(screen.getByLabelText("인증코드")).toBeInTheDocument();
  });

  it("타이머가 '03:00'으로 시작해야 한다", () => {
    const mockOnVerify = vi.fn().mockResolvedValue({ success: true });
    const mockOnResend = vi.fn().mockResolvedValue({ success: true });

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    expect(screen.getByText("03:00")).toBeInTheDocument();
  });
});

describe("PhoneVerification - 타이머 (fake timers)", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("시간이 지나면 타이머가 감소해야 한다", async () => {
    const mockOnVerify = vi.fn().mockResolvedValue({ success: true });
    const mockOnResend = vi.fn().mockResolvedValue({ success: true });

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await act(async () => {
      vi.advanceTimersByTime(1000);
    });

    expect(screen.getByText("02:59")).toBeInTheDocument();
  });

  it("타이머 만료 시 확인 버튼이 비활성화되고 '만료됨'이 표시되어야 한다", async () => {
    const mockOnVerify = vi.fn().mockResolvedValue({ success: true });
    const mockOnResend = vi.fn().mockResolvedValue({ success: true });

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await act(async () => {
      vi.advanceTimersByTime(180 * 1000 + 500);
    });

    expect(screen.getByText("만료됨")).toBeInTheDocument();

    const confirmButton = screen.getByRole("button", { name: "인증 확인" });
    expect(confirmButton).toBeDisabled();
  });
});

describe("PhoneVerification - 인터랙션", () => {
  let mockOnVerify: ReturnType<typeof vi.fn>;
  let mockOnResend: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockOnVerify = vi.fn().mockResolvedValue({ success: true });
    mockOnResend = vi.fn().mockResolvedValue({ success: true });
  });

  it("인증코드를 입력하고 확인 버튼을 클릭하면 onVerify가 호출되어야 한다", async () => {
    const user = userEvent.setup();

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await user.type(screen.getByLabelText("인증코드"), "123456");
    await user.click(screen.getByRole("button", { name: "인증 확인" }));

    await waitFor(() => {
      expect(mockOnVerify).toHaveBeenCalledWith("123456");
    });
  });

  it("인증코드가 6자리 미만이면 오류 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await user.type(screen.getByLabelText("인증코드"), "123");
    await user.click(screen.getByRole("button", { name: "인증 확인" }));

    await waitFor(() => {
      expect(
        screen.getByText("인증코드 6자리를 입력해주세요.")
      ).toBeInTheDocument();
    });
    expect(mockOnVerify).not.toHaveBeenCalled();
  });

  it("재전송 버튼을 클릭하면 onResend가 호출되어야 한다", async () => {
    const user = userEvent.setup();

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await user.click(screen.getByRole("button", { name: "인증코드 재전송" }));

    await waitFor(() => {
      expect(mockOnResend).toHaveBeenCalled();
    });
  });

  it("재전송 성공 시 성공 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await user.click(screen.getByRole("button", { name: "인증코드 재전송" }));

    await waitFor(() => {
      expect(
        screen.getByText("인증코드가 재전송되었습니다.")
      ).toBeInTheDocument();
    });
  });

  it("재전송 실패 시 에러 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    mockOnResend.mockResolvedValue({
      success: false,
      error: "재전송에 실패했습니다.",
    });

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await user.click(screen.getByRole("button", { name: "인증코드 재전송" }));

    await waitFor(() => {
      expect(screen.getByText("재전송에 실패했습니다.")).toBeInTheDocument();
    });
  });

  it("onVerify 실패 시 에러 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    mockOnVerify.mockResolvedValue({
      success: false,
      error: "인증코드가 올바르지 않습니다.",
    });

    render(
      <PhoneVerification
        phone={mockPhone}
        onVerify={mockOnVerify}
        onResend={mockOnResend}
      />
    );

    await user.type(screen.getByLabelText("인증코드"), "999999");
    await user.click(screen.getByRole("button", { name: "인증 확인" }));

    await waitFor(() => {
      expect(
        screen.getByText("인증코드가 올바르지 않습니다.")
      ).toBeInTheDocument();
    });
  });
});
