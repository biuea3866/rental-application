import type { DisputeStatus } from "@/lib/api/types";

// ========================================
// DisputeStatusBadge — 분쟁 상태 뱃지
// ========================================

const STATUS_CONFIG: Record<
  DisputeStatus,
  { label: string; className: string }
> = {
  OPEN: {
    label: "오픈",
    className: "bg-blue-100 text-blue-700",
  },
  UNDER_REVIEW: {
    label: "검토 중",
    className: "bg-yellow-100 text-yellow-700",
  },
  RESOLVED_REFUND: {
    label: "전액 환불",
    className: "bg-green-100 text-green-700",
  },
  RESOLVED_PARTIAL: {
    label: "부분 환불",
    className: "bg-orange-100 text-orange-700",
  },
  RESOLVED_REJECTED: {
    label: "기각",
    className: "bg-gray-100 text-gray-600",
  },
  CANCELLED: {
    label: "취소됨",
    className: "bg-red-100 text-red-600",
  },
};

interface DisputeStatusBadgeProps {
  status: DisputeStatus;
}

export function DisputeStatusBadge({ status }: DisputeStatusBadgeProps) {
  const config = STATUS_CONFIG[status];
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
}
