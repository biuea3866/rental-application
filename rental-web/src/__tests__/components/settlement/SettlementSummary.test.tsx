import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { SettlementSummary } from "@/components/settlement/SettlementSummary";
import type { SettlementResponse } from "@/lib/api/types";

// ========================================
// SettlementSummary 컴포넌트 테스트
// RC-FE-320
// ========================================

const STUB_SETTLEMENTS: SettlementResponse[] = [
  {
    settlementId: 1,
    lenderId: 22,
    rentalId: 1001,
    amount: 120000,
    commissionRate: 0.1,
    commissionAmount: 12000,
    netAmount: 108000,
    status: "COMPLETED",
    createdAt: "2026-04-12T10:00:00+09:00",
  },
  {
    settlementId: 2,
    lenderId: 22,
    rentalId: 1002,
    amount: 75000,
    commissionRate: 0.1,
    commissionAmount: 7500,
    netAmount: 67500,
    status: "PENDING",
    createdAt: "2026-04-14T11:00:00+09:00",
  },
  {
    settlementId: 3,
    lenderId: 22,
    rentalId: 1003,
    amount: 30000,
    commissionRate: 0.1,
    commissionAmount: 3000,
    netAmount: 27000,
    status: "COMPLETED",
    createdAt: "2026-04-09T09:00:00+09:00",
  },
];

describe("SettlementSummary", () => {
  it("요약 컴포넌트가 렌더링되어야 한다", () => {
    render(<SettlementSummary settlements={STUB_SETTLEMENTS} />);
    expect(screen.getByTestId("settlement-summary")).toBeInTheDocument();
  });

  it("PENDING 합계가 올바르게 계산되어야 한다", () => {
    render(<SettlementSummary settlements={STUB_SETTLEMENTS} />);
    // PENDING: 67,500원
    expect(screen.getByTestId("pending-total")).toHaveTextContent("67,500원");
  });

  it("COMPLETED 합계가 올바르게 계산되어야 한다", () => {
    render(<SettlementSummary settlements={STUB_SETTLEMENTS} />);
    // COMPLETED: 108,000 + 27,000 = 135,000원
    expect(screen.getByTestId("completed-total")).toHaveTextContent("135,000원");
  });

  it("빈 배열일 때 0원으로 표시되어야 한다", () => {
    render(<SettlementSummary settlements={[]} />);
    const totals = screen.getAllByText("0원");
    expect(totals).toHaveLength(2);
  });
});
