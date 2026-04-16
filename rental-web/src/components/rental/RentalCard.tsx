"use client";

import Link from "next/link";
import type { RentalSummary, RentalStatus } from "@/lib/api/types";

// ========================================
// 대여 카드 컴포넌트
// PRD-002: 5.3 내 대여 목록 — 목록 카드 구성
// ========================================

const STATUS_BADGE_STYLES: Record<RentalStatus, { bg: string; text: string; label: string }> = {
  REQUESTED: { bg: "bg-gray-100", text: "text-gray-600", label: "대기 중" },
  APPROVED: { bg: "bg-blue-100", text: "text-blue-700", label: "승인됨" },
  PAID: { bg: "bg-green-100", text: "text-green-700", label: "결제 완료" },
  IN_USE: { bg: "bg-orange-100", text: "text-orange-700", label: "대여 중" },
  RETURNED: { bg: "bg-slate-100", text: "text-slate-600", label: "반납 완료" },
  CANCELLED: { bg: "bg-red-100", text: "text-red-700", label: "취소됨" },
};

function formatDateRange(startDate: string, endDate: string): string {
  const start = new Date(startDate);
  const end = new Date(endDate);

  const startFormatted = start.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  const endFormatted = end.toLocaleDateString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
  });

  return `${startFormatted} ~ ${endFormatted}`;
}

function formatAmount(amount: number): string {
  return amount.toLocaleString("ko-KR") + "원";
}

interface RentalCardProps {
  rental: RentalSummary;
}

export function RentalCard({ rental }: RentalCardProps) {
  const badge = STATUS_BADGE_STYLES[rental.status];

  return (
    <Link
      href={`/rentals/${rental.rentalId}`}
      data-testid={`rental-card-${rental.rentalId}`}
      className="flex items-center gap-4 p-4 bg-white rounded-lg border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all"
    >
      {/* 상품 이미지 */}
      <div className="w-[60px] h-[60px] flex-shrink-0 rounded-md overflow-hidden bg-gray-100">
        {rental.productThumbnailUrl ? (
          <img
            src={rental.productThumbnailUrl}
            alt={rental.productName}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">
            이미지 없음
          </div>
        )}
      </div>

      {/* 상품 정보 */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 truncate">
          {rental.productName}
        </p>
        <p className="text-xs text-gray-500 mt-0.5">
          {formatDateRange(rental.startDate, rental.endDate)}
        </p>
        <div className="flex items-center gap-2 mt-1">
          <span
            data-testid={`rental-status-badge-${rental.rentalId}`}
            className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${badge.bg} ${badge.text}`}
          >
            {badge.label}
          </span>
          <span className="text-xs font-semibold text-gray-700">
            {formatAmount(rental.totalAmount)}
          </span>
        </div>
      </div>

      {/* 화살표 */}
      <svg
        className="w-4 h-4 text-gray-400 flex-shrink-0"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M9 5l7 7-7 7"
        />
      </svg>
    </Link>
  );
}
