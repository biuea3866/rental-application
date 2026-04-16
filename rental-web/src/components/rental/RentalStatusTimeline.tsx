"use client";

import type { RentalStatus, RentalDetail } from "@/lib/api/types";

// ========================================
// 대여 상태 타임라인 컴포넌트
// PRD-002: 5.4 대여 상세 페이지 — 상태 타임라인 표시
// ========================================

const STATUS_STEPS: { status: RentalStatus; label: string }[] = [
  { status: "REQUESTED", label: "신청" },
  { status: "APPROVED", label: "승인" },
  { status: "PAID", label: "결제" },
  { status: "IN_USE", label: "대여중" },
  { status: "RETURNED", label: "반납" },
];

const STATUS_ORDER: Record<RentalStatus, number> = {
  REQUESTED: 0,
  APPROVED: 1,
  PAID: 2,
  IN_USE: 3,
  RETURNED: 4,
  CANCELLED: -1,
};

function formatDateTime(dateString?: string): string {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getStepTimestamp(rental: RentalDetail, status: RentalStatus): string {
  switch (status) {
    case "REQUESTED":
      return formatDateTime(rental.requestedAt);
    case "APPROVED":
      return formatDateTime(rental.approvedAt);
    case "PAID":
      return formatDateTime(rental.paidAt);
    case "IN_USE":
      return formatDateTime(rental.startedAt);
    case "RETURNED":
      return formatDateTime(rental.returnedAt);
    default:
      return "";
  }
}

interface RentalStatusTimelineProps {
  rental: RentalDetail;
}

export function RentalStatusTimeline({ rental }: RentalStatusTimelineProps) {
  const isCancelled = rental.status === "CANCELLED";
  const currentOrder = STATUS_ORDER[rental.status];

  if (isCancelled) {
    return (
      <div
        data-testid="rental-status-timeline"
        className="bg-red-50 border border-red-200 rounded-lg p-4"
      >
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-red-500" />
          <span className="text-sm font-medium text-red-700">취소됨</span>
        </div>
        {rental.cancelReason && (
          <p className="mt-2 text-sm text-red-600">사유: {rental.cancelReason}</p>
        )}
        {rental.cancelledAt && (
          <p className="mt-1 text-xs text-red-500">
            {formatDateTime(rental.cancelledAt)}
          </p>
        )}
      </div>
    );
  }

  return (
    <div data-testid="rental-status-timeline" className="py-2">
      <div className="relative flex items-start justify-between">
        {/* 연결선 */}
        <div className="absolute top-3 left-3 right-3 h-0.5 bg-gray-200" />
        <div
          className="absolute top-3 left-3 h-0.5 bg-blue-500 transition-all duration-500"
          style={{
            width: `${(currentOrder / (STATUS_STEPS.length - 1)) * 100}%`,
          }}
        />

        {STATUS_STEPS.map((step, index) => {
          const stepOrder = STATUS_ORDER[step.status];
          const isCompleted = stepOrder < currentOrder;
          const isActive = stepOrder === currentOrder;
          const timestamp = getStepTimestamp(rental, step.status);

          return (
            <div
              key={step.status}
              className="relative flex flex-col items-center"
              style={{ width: `${100 / STATUS_STEPS.length}%` }}
              data-testid={`timeline-step-${step.status}`}
            >
              {/* 스텝 원 */}
              <div
                className={[
                  "w-6 h-6 rounded-full border-2 flex items-center justify-center z-10 bg-white",
                  isCompleted
                    ? "border-blue-500 bg-blue-500"
                    : isActive
                      ? "border-blue-500 bg-blue-100"
                      : "border-gray-300",
                ].join(" ")}
              >
                {isCompleted && (
                  <svg
                    className="w-3 h-3 text-white"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                    aria-hidden="true"
                  >
                    <path
                      fillRule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                )}
                {isActive && (
                  <div className="w-2 h-2 rounded-full bg-blue-500" />
                )}
              </div>

              {/* 라벨 */}
              <span
                className={[
                  "mt-2 text-xs font-medium text-center",
                  isCompleted || isActive ? "text-blue-600" : "text-gray-400",
                ].join(" ")}
              >
                {step.label}
              </span>

              {/* 타임스탬프 */}
              {timestamp && (
                <span className="mt-1 text-xs text-gray-400 text-center leading-tight">
                  {timestamp}
                </span>
              )}

              {/* 숨김 처리: 첫 번째 아이템의 인덱스 사용 방지 */}
              <span className="sr-only">{index}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
