"use client";

// ========================================
// 대여 상태별 카드 컴포넌트 (2x3 그리드)
// ========================================

const STATUS_LABELS: Record<string, string> = {
  REQUESTED: "대기 중",
  APPROVED: "승인됨",
  PAID: "결제 완료",
  IN_USE: "대여 중",
  RETURNED: "반납 완료",
  CANCELLED: "취소됨",
};

const STATUS_COLORS: Record<string, string> = {
  REQUESTED: "bg-yellow-50 border-yellow-200 text-yellow-700",
  APPROVED: "bg-blue-50 border-blue-200 text-blue-700",
  PAID: "bg-indigo-50 border-indigo-200 text-indigo-700",
  IN_USE: "bg-green-50 border-green-200 text-green-700",
  RETURNED: "bg-gray-50 border-gray-200 text-gray-700",
  CANCELLED: "bg-red-50 border-red-200 text-red-700",
};

const STATUS_COUNT_COLORS: Record<string, string> = {
  REQUESTED: "text-yellow-800",
  APPROVED: "text-blue-800",
  PAID: "text-indigo-800",
  IN_USE: "text-green-800",
  RETURNED: "text-gray-800",
  CANCELLED: "text-red-800",
};

interface RentalStatusCardsProps {
  counts: Record<string, number>;
}

export function RentalStatusCards({ counts }: RentalStatusCardsProps) {
  const statuses = [
    "REQUESTED",
    "APPROVED",
    "PAID",
    "IN_USE",
    "RETURNED",
    "CANCELLED",
  ];

  return (
    <div
      data-testid="rental-status-cards"
      className="grid grid-cols-2 sm:grid-cols-3 gap-3"
    >
      {statuses.map((status) => (
        <div
          key={status}
          data-testid={`status-card-${status}`}
          className={`border rounded-xl p-4 ${STATUS_COLORS[status]}`}
        >
          <p className="text-xs font-medium mb-2">{STATUS_LABELS[status]}</p>
          <p
            className={`text-2xl font-bold ${STATUS_COUNT_COLORS[status]}`}
          >
            {(counts[status] ?? 0).toLocaleString()}
          </p>
          <p className="text-xs mt-1 opacity-70">건</p>
        </div>
      ))}
    </div>
  );
}
