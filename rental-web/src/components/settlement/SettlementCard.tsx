"use client";

import type { SettlementResponse, SettlementStatus } from "@/lib/api/types";

// ========================================
// SettlementCard 컴포넌트
// RC-FE-320
// ========================================

const STATUS_STYLES: Record<SettlementStatus, { bg: string; text: string; label: string }> = {
  PENDING: { bg: "bg-yellow-100", text: "text-yellow-700", label: "정산 대기" },
  COMPLETED: { bg: "bg-green-100", text: "text-green-700", label: "정산 완료" },
};

function formatAmount(amount: number): string {
  return amount.toLocaleString("ko-KR") + "원";
}

function formatDate(dateString: string | null): string {
  if (!dateString) return "-";
  const date = new Date(dateString);
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

interface SettlementCardProps {
  settlement: SettlementResponse;
}

export function SettlementCard({ settlement }: SettlementCardProps) {
  const statusStyle = STATUS_STYLES[settlement.status];

  return (
    <div
      data-testid={`settlement-card-${settlement.settlementId}`}
      className="bg-white rounded-lg border border-gray-200 p-4 space-y-3"
    >
      {/* 헤더 — 상태 + 날짜 */}
      <div className="flex items-center justify-between">
        <span
          data-testid={`settlement-status-${settlement.settlementId}`}
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${statusStyle.bg} ${statusStyle.text}`}
        >
          {statusStyle.label}
        </span>
        {/* BE 필드: settledAt (nullable) */}
        <span className="text-xs text-gray-400">{formatDate(settlement.settledAt)}</span>
      </div>

      {/* 대여 ID */}
      <div className="text-xs text-gray-500">
        대여 #{settlement.rentalId}
      </div>

      {/* 금액 상세 */}
      <div className="space-y-1.5 pt-1 border-t border-gray-100">
        <div className="flex justify-between text-sm">
          <span className="text-gray-500">대여 금액</span>
          <span className="text-gray-900 font-medium">{formatAmount(settlement.amount)}</span>
        </div>
        {/* BE 필드: commission (단일 금액, commissionRate/commissionAmount 없음) */}
        <div className="flex justify-between text-sm">
          <span className="text-gray-500">수수료</span>
          <span className="text-red-500">-{formatAmount(settlement.commission)}</span>
        </div>
        <div className="flex justify-between text-sm font-semibold pt-1 border-t border-gray-100">
          <span className="text-gray-700">실수령 금액</span>
          <span
            data-testid={`settlement-net-amount-${settlement.settlementId}`}
            className="text-blue-600"
          >
            {formatAmount(settlement.netAmount)}
          </span>
        </div>
      </div>
    </div>
  );
}
