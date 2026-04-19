"use client";

import { useQuery } from "@tanstack/react-query";
import { ReviewCard } from "@/components/review/ReviewCard";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { ReviewListResponse } from "@/lib/api/types";

// ========================================
// 내 리뷰 목록 페이지
// RC-FE-319
// ========================================

export default function MyReviewsPage() {
  const apiClient = getApiClient();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["my-reviews"],
    queryFn: async () => {
      const response = await apiClient.get<ReviewListResponse>(
        ENDPOINTS.REVIEWS.MY_REVIEWS,
        { params: { page: "0", size: "20" } }
      );
      return response.data;
    },
  });

  const reviews = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-4">
          <h1 className="text-lg font-semibold text-gray-900">내 리뷰</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-4 space-y-4">
        {/* 총 개수 */}
        {!isLoading && !isError && (
          <div className="text-sm text-gray-500">
            총{" "}
            <span className="font-medium text-gray-900">{totalElements}</span>
            건의 리뷰
          </div>
        )}

        {/* 로딩 상태 */}
        {isLoading && (
          <div data-testid="loading-state" className="space-y-3">
            {[...Array(3)].map((_, index) => (
              <div
                key={index}
                className="h-28 bg-gray-200 rounded-lg animate-pulse"
              />
            ))}
          </div>
        )}

        {/* 에러 상태 */}
        {isError && !isLoading && (
          <div data-testid="error-state" className="text-center py-12">
            <p className="text-gray-500 mb-4">
              리뷰를 불러오는 중 오류가 발생했습니다.
            </p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700"
            >
              다시 시도
            </button>
          </div>
        )}

        {/* 빈 상태 */}
        {!isLoading && !isError && reviews.length === 0 && (
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
                  d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.562.562 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z"
                />
              </svg>
            </div>
            <p className="text-gray-500 text-sm">아직 작성한 리뷰가 없습니다.</p>
          </div>
        )}

        {/* 리뷰 목록 */}
        {!isLoading && !isError && reviews.length > 0 && (
          <div data-testid="review-list" className="space-y-3">
            {reviews.map((review) => (
              <ReviewCard key={review.reviewId} review={review} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
