"use client";

import { useQuery } from "@tanstack/react-query";
import { SettlementCard } from "@/components/settlement/SettlementCard";
import { SettlementSummary } from "@/components/settlement/SettlementSummary";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { SettlementListResponse } from "@/lib/api/types";

// ========================================
// 정산 내역 페이지
// RC-FE-320
// ========================================

export default function SettlementsPage() {
  const apiClient = getApiClient();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["my-settlements"],
    queryFn: async () => {
      const response = await apiClient.get<SettlementListResponse>(
        ENDPOINTS.SETTLEMENTS.MY_SETTLEMENTS,
        { params: { page: "0", size: "20" } }
      );
      return response.data;
    },
  });

  const settlements = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-4">
          <h1 className="text-lg font-semibold text-gray-900">정산 내역</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-4 space-y-4">
        {/* 로딩 상태 */}
        {isLoading && (
          <div data-testid="loading-state" className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="h-20 bg-gray-200 rounded-lg animate-pulse" />
              <div className="h-20 bg-gray-200 rounded-lg animate-pulse" />
            </div>
            {[...Array(3)].map((_, index) => (
              <div
                key={index}
                className="h-36 bg-gray-200 rounded-lg animate-pulse"
              />
            ))}
          </div>
        )}

        {/* 에러 상태 */}
        {isError && !isLoading && (
          <div data-testid="error-state" className="text-center py-12">
            <p className="text-gray-500 mb-4">
              정산 내역을 불러오는 중 오류가 발생했습니다.
            </p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700"
            >
              다시 시도
            </button>
          </div>
        )}

        {/* 정산 내역이 있을 때 */}
        {!isLoading && !isError && settlements.length > 0 && (
          <>
            {/* 요약 */}
            <SettlementSummary settlements={settlements} />

            {/* 총 개수 */}
            <div className="text-sm text-gray-500">
              총{" "}
              <span className="font-medium text-gray-900">{totalElements}</span>
              건의 정산 내역
            </div>

            {/* 목록 */}
            <div data-testid="settlement-list" className="space-y-3">
              {settlements.map((settlement) => (
                <SettlementCard
                  key={settlement.settlementId}
                  settlement={settlement}
                />
              ))}
            </div>
          </>
        )}

        {/* 빈 상태 */}
        {!isLoading && !isError && settlements.length === 0 && (
          <div data-testid="empty-state" className="text-center py-16">
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
                  d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <p className="text-gray-500 text-sm">아직 정산 내역이 없습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
}
