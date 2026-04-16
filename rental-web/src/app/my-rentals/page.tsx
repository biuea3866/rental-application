"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { RentalCard } from "@/components/rental/RentalCard";
import { getMyRentalsApi } from "@/lib/api/rental";
import type { RentalStatus, RentalRole } from "@/lib/api/types";

// ========================================
// 내 대여 목록 페이지
// PRD-002: 5.3 내 대여 목록 — 대여자/등록자 탭 + 상태 필터
// RC-FE-215
// ========================================

const STATUS_FILTER_OPTIONS: { label: string; value: RentalStatus | undefined }[] = [
  { label: "전체", value: undefined },
  { label: "대기 중", value: "REQUESTED" },
  { label: "승인됨", value: "APPROVED" },
  { label: "결제 완료", value: "PAID" },
  { label: "대여 중", value: "IN_USE" },
  { label: "반납 완료", value: "RETURNED" },
  { label: "취소됨", value: "CANCELLED" },
];

export default function MyRentalsPage() {
  const [role, setRole] = useState<RentalRole>("RENTER");
  const [statusFilter, setStatusFilter] = useState<RentalStatus | undefined>(undefined);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["my-rentals", role, statusFilter],
    queryFn: () =>
      getMyRentalsApi({ role, status: statusFilter, page: 0, size: 20 }),
  });

  const rentals = data?.data?.content ?? [];
  const totalCount = data?.data?.totalElements ?? 0;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-4">
          <h1 className="text-lg font-semibold text-gray-900">내 대여 목록</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-4 space-y-4">
        {/* 역할 탭 (대여자 / 등록자) */}
        <div
          data-testid="role-tabs"
          className="flex bg-gray-100 rounded-lg p-1"
        >
          <button
            data-testid="tab-renter"
            onClick={() => setRole("RENTER")}
            className={[
              "flex-1 py-2 text-sm font-medium rounded-md transition-colors",
              role === "RENTER"
                ? "bg-white text-gray-900 shadow-sm"
                : "text-gray-500 hover:text-gray-700",
            ].join(" ")}
          >
            대여자로서
          </button>
          <button
            data-testid="tab-lender"
            onClick={() => setRole("LENDER")}
            className={[
              "flex-1 py-2 text-sm font-medium rounded-md transition-colors",
              role === "LENDER"
                ? "bg-white text-gray-900 shadow-sm"
                : "text-gray-500 hover:text-gray-700",
            ].join(" ")}
          >
            등록자로서
          </button>
        </div>

        {/* 상태 필터 */}
        <div
          data-testid="status-filter"
          className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide"
        >
          {STATUS_FILTER_OPTIONS.map((option) => (
            <button
              key={option.value ?? "all"}
              data-testid={`filter-${option.value ?? "all"}`}
              onClick={() => setStatusFilter(option.value)}
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

        {/* 결과 수 */}
        <div className="text-sm text-gray-500">
          총 <span className="font-medium text-gray-900">{totalCount}</span>건
        </div>

        {/* 로딩 상태 */}
        {isLoading && (
          <div data-testid="loading-state" className="space-y-3">
            {[...Array(3)].map((_, index) => (
              <div
                key={index}
                className="h-20 bg-gray-200 rounded-lg animate-pulse"
              />
            ))}
          </div>
        )}

        {/* 에러 상태 */}
        {isError && !isLoading && (
          <div
            data-testid="error-state"
            className="text-center py-12"
          >
            <p className="text-gray-500 mb-4">데이터를 불러오는 중 오류가 발생했습니다.</p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700"
            >
              다시 시도
            </button>
          </div>
        )}

        {/* 빈 상태 */}
        {!isLoading && !isError && rentals.length === 0 && (
          <div
            data-testid="empty-state"
            className="text-center py-16"
          >
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg
                className="w-8 h-8 text-gray-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                />
              </svg>
            </div>
            <p className="text-gray-500 text-sm">
              {statusFilter
                ? `${STATUS_FILTER_OPTIONS.find((o) => o.value === statusFilter)?.label} 상태의 대여가 없습니다.`
                : "아직 대여 내역이 없습니다."}
            </p>
          </div>
        )}

        {/* 대여 목록 */}
        {!isLoading && !isError && rentals.length > 0 && (
          <div
            data-testid="rental-list"
            className="space-y-3"
          >
            {rentals.map((rental) => (
              <RentalCard key={rental.rentalId} rental={rental} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
