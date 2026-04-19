"use client";

import { useState } from "react";
import { StarRating } from "./StarRating";

// ========================================
// ReviewForm 컴포넌트 — 별점 + 텍스트 리뷰 작성
// RC-FE-317
// ========================================

interface ReviewFormProps {
  onSubmit: (data: { rating: number; content: string }) => void;
  isSubmitting?: boolean;
}

const MIN_CONTENT_LENGTH = 10;
const MAX_CONTENT_LENGTH = 500;

export function ReviewForm({ onSubmit, isSubmitting = false }: ReviewFormProps) {
  const [rating, setRating] = useState(0);
  const [content, setContent] = useState("");

  const contentLength = content.length;
  const isContentValid =
    contentLength >= MIN_CONTENT_LENGTH && contentLength <= MAX_CONTENT_LENGTH;
  const isFormValid = rating > 0 && isContentValid;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isFormValid) return;
    onSubmit({ rating, content });
  }

  return (
    <form
      data-testid="review-form"
      onSubmit={handleSubmit}
      className="space-y-6"
    >
      {/* 별점 */}
      <div className="space-y-2">
        <label className="text-sm font-medium text-gray-700">별점</label>
        <div className="flex items-center gap-3">
          <StarRating value={rating} onChange={setRating} size="lg" />
          {rating > 0 && (
            <span className="text-sm text-gray-500">{rating}점</span>
          )}
        </div>
        {rating === 0 && (
          <p className="text-xs text-gray-400">별점을 선택해주세요</p>
        )}
      </div>

      {/* 리뷰 내용 */}
      <div className="space-y-2">
        <label
          htmlFor="review-content"
          className="text-sm font-medium text-gray-700"
        >
          리뷰 내용
        </label>
        <textarea
          id="review-content"
          data-testid="review-content-input"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="대여 경험을 자세히 남겨주세요 (10~500자)"
          maxLength={MAX_CONTENT_LENGTH}
          rows={5}
          className={[
            "w-full px-3 py-2 rounded-lg border text-sm resize-none",
            "focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent",
            "placeholder:text-gray-400",
            !isContentValid && contentLength > 0
              ? "border-red-300"
              : "border-gray-300",
          ].join(" ")}
        />
        <div className="flex justify-between items-center">
          <span
            className={[
              "text-xs",
              !isContentValid && contentLength > 0
                ? "text-red-500"
                : "text-gray-400",
            ].join(" ")}
          >
            {contentLength < MIN_CONTENT_LENGTH && contentLength > 0
              ? `최소 ${MIN_CONTENT_LENGTH}자 이상 입력해주세요`
              : ""}
          </span>
          <span className="text-xs text-gray-400">
            {contentLength}/{MAX_CONTENT_LENGTH}
          </span>
        </div>
      </div>

      {/* 제출 버튼 */}
      <button
        type="submit"
        data-testid="review-submit-button"
        disabled={!isFormValid || isSubmitting}
        className={[
          "w-full py-3 rounded-lg text-sm font-semibold transition-colors",
          isFormValid && !isSubmitting
            ? "bg-blue-600 text-white hover:bg-blue-700 active:bg-blue-800"
            : "bg-gray-200 text-gray-400 cursor-not-allowed",
        ].join(" ")}
      >
        {isSubmitting ? "제출 중..." : "리뷰 등록"}
      </button>
    </form>
  );
}
