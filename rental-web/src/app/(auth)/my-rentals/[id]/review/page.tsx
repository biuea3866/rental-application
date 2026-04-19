"use client";

import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { ReviewForm } from "@/components/review/ReviewForm";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { ReviewResponse, CreateReviewRequest } from "@/lib/api/types";

// ========================================
// 리뷰 작성 페이지
// RC-FE-317
// ========================================

export default function WriteReviewPage() {
  const params = useParams<{ id: string }>();
  const searchParams = useSearchParams();
  const router = useRouter();
  const apiClient = getApiClient();

  const rentalId = Number(params.id);
  const productId = Number(searchParams.get("productId") ?? "0");

  const { mutate, isPending, isError, error } = useMutation({
    mutationFn: async (data: { rating: number; content: string }) => {
      const body: CreateReviewRequest = {
        rentalId,
        productId,
        rating: data.rating,
        content: data.content,
      };
      const response = await apiClient.post<ReviewResponse>(
        ENDPOINTS.REVIEWS.BASE,
        body
      );
      return response.data;
    },
    onSuccess: () => {
      router.push("/my-rentals");
    },
  });

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-4 flex items-center gap-3">
          <button
            onClick={() => router.back()}
            aria-label="뒤로가기"
            className="p-1 rounded-md hover:bg-gray-100 transition-colors"
          >
            <svg
              className="w-5 h-5 text-gray-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>
          <h1 className="text-lg font-semibold text-gray-900">리뷰 작성</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
        {/* 안내 문구 */}
        <div className="bg-blue-50 rounded-lg border border-blue-200 p-4">
          <p className="text-sm text-blue-700">
            대여 #{rentalId} 에 대한 솔직한 리뷰를 남겨주세요.
            다른 이용자에게 큰 도움이 됩니다.
          </p>
        </div>

        {/* 에러 메시지 */}
        {isError && (
          <div
            data-testid="review-error"
            className="bg-red-50 rounded-lg border border-red-200 p-4"
          >
            <p className="text-sm text-red-700">
              {error instanceof Error
                ? error.message
                : "리뷰 등록 중 오류가 발생했습니다. 다시 시도해주세요."}
            </p>
          </div>
        )}

        {/* 리뷰 폼 */}
        <ReviewForm onSubmit={mutate} isSubmitting={isPending} />
      </div>
    </div>
  );
}
