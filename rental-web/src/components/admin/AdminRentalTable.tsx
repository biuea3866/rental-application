"use client";

import { useState } from "react";
import type { AdminRentalResponse, RentalStatus } from "@/lib/api/types";

// ========================================
// 관리자 대여 목록 테이블
// ========================================

const STATUS_LABELS: Record<RentalStatus, string> = {
  REQUESTED: "대기 중",
  APPROVED: "승인됨",
  PAID: "결제 완료",
  IN_USE: "대여 중",
  RETURNED: "반납 완료",
  CANCELLED: "취소됨",
};

const STATUS_BADGE_COLORS: Record<RentalStatus, string> = {
  REQUESTED: "bg-yellow-100 text-yellow-700",
  APPROVED: "bg-blue-100 text-blue-700",
  PAID: "bg-indigo-100 text-indigo-700",
  IN_USE: "bg-green-100 text-green-700",
  RETURNED: "bg-gray-100 text-gray-700",
  CANCELLED: "bg-red-100 text-red-700",
};

const STATUS_FILTER_OPTIONS: { label: string; value: RentalStatus | undefined }[] =
  [
    { label: "전체", value: undefined },
    { label: "대기 중", value: "REQUESTED" },
    { label: "승인됨", value: "APPROVED" },
    { label: "결제 완료", value: "PAID" },
    { label: "대여 중", value: "IN_USE" },
    { label: "반납 완료", value: "RETURNED" },
    { label: "취소됨", value: "CANCELLED" },
  ];

interface AdminRentalTableProps {
  rentals: AdminRentalResponse[];
  isLoading?: boolean;
  onStatusFilterChange?: (status: RentalStatus | undefined) => void;
}

export function AdminRentalTable({
  rentals,
  isLoading,
  onStatusFilterChange,
}: AdminRentalTableProps) {
  const [statusFilter, setStatusFilter] = useState<RentalStatus | undefined>(
    undefined
  );

  const handleFilterChange = (status: RentalStatus | undefined) => {
    setStatusFilter(status);
    onStatusFilterChange?.(status);
  };

  return (
    <div data-testid="admin-rental-table" className="space-y-3">
      {/* 상태 필터 */}
      <div className="flex gap-2 overflow-x-auto pb-1">
        {STATUS_FILTER_OPTIONS.map((option) => (
          <button
            key={option.value ?? "all"}
            data-testid={`rental-filter-${option.value ?? "all"}`}
            onClick={() => handleFilterChange(option.value)}
            className={[
              "flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-colors border",
              statusFilter === option.value
                ? "bg-blue-600 text-white border-blue-600"
                : "bg-white text-gray-600 border-gray-300 hover:border-blue-400",
            ].join(" ")}
          >
            {option.label}
          </button>
        ))}
      </div>

      {/* 테이블 (모바일: 카드형, 데스크탑: 테이블) */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {/* 데스크탑 테이블 헤더 */}
        <div className="hidden md:grid grid-cols-6 gap-4 px-4 py-3 bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-500">
          <span>대여 ID</span>
          <span>상품명</span>
          <span>대여자 ID</span>
          <span>상태</span>
          <span>금액</span>
          <span>생성일</span>
        </div>

        {isLoading && (
          <div className="space-y-px">
            {[...Array(5)].map((_, i) => (
              <div
                key={i}
                className="h-16 bg-gray-50 animate-pulse border-b border-gray-100"
              />
            ))}
          </div>
        )}

        {!isLoading && rentals.length === 0 && (
          <div
            data-testid="rental-table-empty"
            className="text-center py-12 text-sm text-gray-400"
          >
            대여 내역이 없습니다.
          </div>
        )}

        {!isLoading &&
          rentals.map((rental) => (
            <div
              key={rental.rentalId}
              data-testid={`rental-row-${rental.rentalId}`}
              className="px-4 py-3 border-b border-gray-100 last:border-0 hover:bg-gray-50"
            >
              {/* 모바일: 카드형 */}
              <div className="md:hidden space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-900">
                    #{rental.rentalId} {rental.productName}
                  </span>
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_BADGE_COLORS[rental.status]}`}
                  >
                    {STATUS_LABELS[rental.status]}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs text-gray-500">
                  <span>대여자 #{rental.renterId}</span>
                  <span>{rental.totalAmount.toLocaleString()}원</span>
                </div>
                <p className="text-xs text-gray-400">
                  {new Date(rental.createdAt).toLocaleDateString("ko-KR")}
                </p>
              </div>

              {/* 데스크탑: 그리드형 */}
              <div className="hidden md:grid grid-cols-6 gap-4 items-center text-sm text-gray-700">
                <span className="font-medium">#{rental.rentalId}</span>
                <span className="truncate">{rental.productName}</span>
                <span>#{rental.renterId}</span>
                <span>
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_BADGE_COLORS[rental.status]}`}
                  >
                    {STATUS_LABELS[rental.status]}
                  </span>
                </span>
                <span>{rental.totalAmount.toLocaleString()}원</span>
                <span className="text-gray-400 text-xs">
                  {new Date(rental.createdAt).toLocaleDateString("ko-KR")}
                </span>
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}
