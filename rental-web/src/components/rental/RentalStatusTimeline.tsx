"use client";

import { cn } from "@/lib/utils";
import type { RentalStatus } from "@/lib/api/types";

// ========================================
// 상태 단계 정의
// ========================================

interface TimelineStep {
  status: RentalStatus;
  label: string;
  dateKey: keyof TimelineDates;
}

interface TimelineDates {
  requestedAt?: string;
  approvedAt?: string;
  paidAt?: string;
  startedAt?: string;
  returnedAt?: string;
  cancelledAt?: string;
}

const TIMELINE_STEPS: TimelineStep[] = [
  { status: "REQUESTED", label: "신청", dateKey: "requestedAt" },
  { status: "APPROVED", label: "승인", dateKey: "approvedAt" },
  { status: "PAID", label: "결제", dateKey: "paidAt" },
  { status: "IN_USE", label: "대여 중", dateKey: "startedAt" },
  { status: "RETURNED", label: "반납", dateKey: "returnedAt" },
];

const STATUS_ORDER: Record<RentalStatus, number> = {
  REQUESTED: 0,
  APPROVED: 1,
  PAID: 2,
  IN_USE: 3,
  RETURNED: 4,
  CANCELLED: -1,
};

// ========================================
// RentalStatusTimeline 컴포넌트
// ========================================

interface RentalStatusTimelineProps {
  status: RentalStatus;
  dates: TimelineDates;
  className?: string;
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return `${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

export function RentalStatusTimeline({
  status,
  dates,
  className,
}: RentalStatusTimelineProps) {
  const currentOrder = STATUS_ORDER[status] ?? 0;
  const isCancelled = status === "CANCELLED";

  return (
    <div
      data-testid="rental-status-timeline"
      className={cn("w-full", className)}
    >
      {isCancelled ? (
        <div className="flex items-center gap-2 p-3 rounded-lg bg-red-50 border border-red-200">
          <span
            data-testid="timeline-cancelled"
            className="text-red-600 font-semibold text-sm"
          >
            취소됨
          </span>
          {dates.cancelledAt && (
            <span className="text-xs text-red-400">
              {formatDate(dates.cancelledAt)}
            </span>
          )}
        </div>
      ) : (
        <div className="flex items-start gap-0">
          {TIMELINE_STEPS.map((step, index) => {
            const stepOrder = STATUS_ORDER[step.status];
            const isCompleted = stepOrder <= currentOrder;
            const isCurrent = stepOrder === currentOrder;
            const isLast = index === TIMELINE_STEPS.length - 1;
            const dateStr = dates[step.dateKey];

            return (
              <div
                key={step.status}
                data-testid={`timeline-step-${step.status}`}
                className="flex flex-col items-center flex-1"
              >
                {/* 스텝 헤더: 원 + 연결선 */}
                <div className="flex items-center w-full">
                  <div
                    className={cn(
                      "w-4 h-4 rounded-full border-2 shrink-0 z-10",
                      isCompleted
                        ? "bg-primary border-primary"
                        : "bg-white border-muted-foreground/30",
                      isCurrent && "ring-2 ring-primary/30"
                    )}
                    aria-label={step.label}
                  />
                  {!isLast && (
                    <div
                      className={cn(
                        "flex-1 h-0.5",
                        stepOrder < currentOrder
                          ? "bg-primary"
                          : "bg-muted-foreground/20"
                      )}
                    />
                  )}
                </div>

                {/* 스텝 라벨 + 날짜 */}
                <div className="mt-1 text-center px-0.5 w-full">
                  <span
                    className={cn(
                      "text-xs font-medium block",
                      isCompleted ? "text-primary" : "text-muted-foreground/60"
                    )}
                  >
                    {step.label}
                  </span>
                  {dateStr && (
                    <span className="text-[10px] text-muted-foreground block leading-tight">
                      {formatDate(dateStr)}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
