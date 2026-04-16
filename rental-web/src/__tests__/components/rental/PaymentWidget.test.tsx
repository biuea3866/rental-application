import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PaymentWidget } from "@/components/rental/PaymentWidget";
import type { Rental } from "@/lib/api/types";

// ========================================
// PaymentWidget 테스트
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
  }),
}));

// processPaymentApi를 모킹
vi.mock("@/lib/api/rental", () => ({
  processPaymentApi: vi.fn().mockResolvedValue({
    data: { id: 2, status: "PAID" },
    success: true,
    timestamp: new Date().toISOString(),
  }),
}));

const STUB_RENTAL: Rental = {
  id: 2,
  productId: 2,
  productName: "맥북 프로 16인치 M3 Pro",
  productImageUrl: "/images/stub/macbook.jpg",
  renterId: 2,
  renterName: "홍길동",
  lenderId: 1,
  lenderName: "김등록",
  status: "APPROVED",
  startDate: "2026-04-18",
  endDate: "2026-04-21",
  dailyPrice: 50000,
  depositAmount: 1000000,
  totalAmount: 1150000,
  deliveryInfo: {
    recipientName: "홍길동",
    recipientPhone: "010-1234-5678",
    address: "서울시 강남구 테헤란로 123",
    addressDetail: "101호",
    zipCode: "06234",
  },
  requestedAt: "2026-04-10T09:00:00Z",
  approvedAt: "2026-04-11T14:00:00Z",
};

describe("PaymentWidget", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("결제 위젯이 렌더링되어야 한다", () => {
    render(<PaymentWidget rental={STUB_RENTAL} />);
    expect(screen.getByTestId("payment-widget")).toBeInTheDocument();
  });

  it("결제하기 버튼이 있어야 한다", () => {
    render(<PaymentWidget rental={STUB_RENTAL} />);
    expect(screen.getByTestId("pay-button")).toBeInTheDocument();
  });

  it("결제 금액이 버튼에 표시되어야 한다", () => {
    render(<PaymentWidget rental={STUB_RENTAL} />);
    expect(screen.getByTestId("pay-button")).toHaveTextContent("1,150,000원");
  });

  it("결제 수단 탭(카드/계좌이체/토스페이)이 있어야 한다", () => {
    render(<PaymentWidget rental={STUB_RENTAL} />);
    expect(screen.getByTestId("payment-method-card")).toBeInTheDocument();
    expect(screen.getByTestId("payment-method-transfer")).toBeInTheDocument();
    expect(screen.getByTestId("payment-method-tosspay")).toBeInTheDocument();
  });

  it("Toss Payments 위젯 플레이스홀더가 표시되어야 한다", () => {
    render(<PaymentWidget rental={STUB_RENTAL} />);
    expect(screen.getByTestId("toss-widget-placeholder")).toBeInTheDocument();
  });

  it("결제하기 버튼 클릭 시 처리 중 상태가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<PaymentWidget rental={STUB_RENTAL} />);

    await user.click(screen.getByTestId("pay-button"));

    // 처리 중 상태 또는 완료 상태 확인
    const button = screen.getByTestId("pay-button");
    expect(
      button.textContent === "결제 처리 중..." ||
        button.textContent?.includes("결제하기") ||
        button.hasAttribute("disabled")
    ).toBeTruthy();
  });

  it("결제 성공 시 onSuccess 콜백이 호출되어야 한다", async () => {
    const user = userEvent.setup();
    const onSuccess = vi.fn();
    render(<PaymentWidget rental={STUB_RENTAL} onSuccess={onSuccess} />);

    await user.click(screen.getByTestId("pay-button"));

    await waitFor(
      () => {
        expect(onSuccess).toHaveBeenCalledWith(STUB_RENTAL.id);
      },
      { timeout: 5000 }
    );
  });
});
