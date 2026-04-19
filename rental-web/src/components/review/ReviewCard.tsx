"use client";

import { StarRating } from "./StarRating";
import type { ReviewResponse } from "@/lib/api/types";

// ========================================
// ReviewCard 컴포넌트 — 별점·내용·날짜 카드
// RC-FE-317
// ========================================

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

interface ReviewCardProps {
  review: ReviewResponse;
}

export function ReviewCard({ review }: ReviewCardProps) {
  return (
    <div
      data-testid={`review-card-${review.reviewId}`}
      className="bg-white rounded-lg border border-gray-200 p-4 space-y-3"
    >
      {/* 별점 및 날짜 */}
      <div className="flex items-center justify-between">
        <StarRating value={review.rating} readonly size="sm" />
        <span
          data-testid="review-date"
          className="text-xs text-gray-400"
        >
          {formatDate(review.createdAt)}
        </span>
      </div>

      {/* 리뷰 내용 */}
      <p
        data-testid="review-content"
        className="text-sm text-gray-700 leading-relaxed"
      >
        {review.content}
      </p>

      {/* 대여 ID 태그 */}
      <div className="text-xs text-gray-400">
        대여 #{review.rentalId}
      </div>
    </div>
  );
}
