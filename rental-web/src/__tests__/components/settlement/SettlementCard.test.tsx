import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { SettlementCard } from "@/components/settlement/SettlementCard";
import type { SettlementResponse } from "@/lib/api/types";

// ========================================
// SettlementCard 컴포넌트 테스트
// RC-FE-320
// ========================================

const STUB_SETTLEMENT_COMPLETED: SettlementResponse = {
  settlementId: 1,
  lenderId: 22,
  rentalId: 1001,
  amount: 120000,
  commissionRate: 0.1,
  commissionAmount: 12000,
  netAmount: 108000,
  status: "COMPLETED",
  createdAt: "2026-04-12T10:00:00+09:00",
};

const STUB_SETTLEMENT_PENDING: SettlementResponse = {
  settlementId: 2,
  lenderId: 22,
  rentalId: 1002,
  amount: 75000,
  commissionRate: 0.1,
  commissionAmount: 7500,
  netAmount: 67500,
  status: "PENDING",
  createdAt: "2026-04-14T11:00:00+09:00",
};

describe("SettlementCard", () => {
  it("정산 카드가 렌더링되어야 한다", () => {
    render(<SettlementCard settlement={STUB_SETTLEMENT_COMPLETED} />);
    expect(screen.getByTestId("settlement-card-1")).toBeInTheDocument();
  });

  it("COMPLETED 상태 뱃지가 초록색으로 표시되어야 한다", () => {
    render(<SettlementCard settlement={STUB_SETTLEMENT_COMPLETED} />);
    const badge = screen.getByTestId("settlement-status-1");
    expect(badge).toHaveTextContent("정산 완료");
    expect(badge.className).toContain("bg-green-100");
  });

  it("PENDING 상태 뱃지가 노란색으로 표시되어야 한다", () => {
    render(<SettlementCard settlement={STUB_SETTLEMENT_PENDING} />);
    const badge = screen.getByTestId("settlement-status-2");
    expect(badge).toHaveTextContent("정산 대기");
    expect(badge.className).toContain("bg-yellow-100");
  });

  it("실수령 금액이 포맷팅되어 표시되어야 한다", () => {
    render(<SettlementCard settlement={STUB_SETTLEMENT_COMPLETED} />);
    expect(
      screen.getByTestId("settlement-net-amount-1")
    ).toHaveTextContent("108,000원");
  });

  it("대여 ID가 표시되어야 한다", () => {
    render(<SettlementCard settlement={STUB_SETTLEMENT_COMPLETED} />);
    expect(screen.getByText("대여 #1001")).toBeInTheDocument();
  });
});
