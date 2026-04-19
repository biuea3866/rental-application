"use client";

import type { SettlementResponse } from "@/lib/api/types";

// ========================================
// SettlementSummary 컴포넌트 — 대기/완료 합계 표시
// RC-FE-320
// ========================================

function formatAmount(amount: number): string {
  return amount.toLocaleString("ko-KR") + "원";
}

interface SettlementSummaryProps {
  settlements: SettlementResponse[];
}

export function SettlementSummary({ settlements }: SettlementSummaryProps) {
  const pendingTotal = settlements
    .filter((s) => s.status === "PENDING")
    .reduce((acc, s) => acc + s.netAmount, 0);

  const completedTotal = settlements
    .filter((s) => s.status === "COMPLETED")
    .reduce((acc, s) => acc + s.netAmount, 0);

  return (
    <div
      data-testid="settlement-summary"
      className="grid grid-cols-2 gap-3"
    >
      {/* 정산 대기 */}
      <div className="bg-yellow-50 rounded-lg border border-yellow-200 p-4 space-y-1">
        <p className="text-xs text-yellow-700 font-medium">정산 대기</p>
        <p
          data-testid="pending-total"
          className="text-lg font-bold text-yellow-800"
        >
          {formatAmount(pendingTotal)}
        </p>
      </div>

      {/* 정산 완료 */}
      <div className="bg-green-50 rounded-lg border border-green-200 p-4 space-y-1">
        <p className="text-xs text-green-700 font-medium">정산 완료</p>
        <p
          data-testid="completed-total"
          className="text-lg font-bold text-green-800"
        >
          {formatAmount(completedTotal)}
        </p>
      </div>
    </div>
  );
}
