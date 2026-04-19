import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { ReviewCard } from "@/components/review/ReviewCard";
import type { ReviewResponse } from "@/lib/api/types";

// ========================================
// ReviewCard 컴포넌트 테스트
// RC-FE-317
// ========================================

const STUB_REVIEW: ReviewResponse = {
  reviewId: 1,
  renterId: 11,
  rentalId: 1001,
  productId: 42,
  rating: 5,
  content: "정말 좋은 제품이었습니다. 깨끗하고 만족스러웠어요.",
  createdAt: "2026-04-10T14:00:00+09:00",
};

describe("ReviewCard", () => {
  it("리뷰 카드가 렌더링되어야 한다", () => {
    render(<ReviewCard review={STUB_REVIEW} />);
    expect(screen.getByTestId("review-card-1")).toBeInTheDocument();
  });

  it("리뷰 내용이 표시되어야 한다", () => {
    render(<ReviewCard review={STUB_REVIEW} />);
    expect(
      screen.getByText("정말 좋은 제품이었습니다. 깨끗하고 만족스러웠어요.")
    ).toBeInTheDocument();
  });

  it("대여 ID가 표시되어야 한다", () => {
    render(<ReviewCard review={STUB_REVIEW} />);
    expect(screen.getByText("대여 #1001")).toBeInTheDocument();
  });

  it("날짜가 표시되어야 한다", () => {
    render(<ReviewCard review={STUB_REVIEW} />);
    expect(screen.getByTestId("review-date")).toBeInTheDocument();
  });

  it("별점 컴포넌트가 렌더링되어야 한다", () => {
    render(<ReviewCard review={STUB_REVIEW} />);
    expect(screen.getByTestId("star-rating")).toBeInTheDocument();
  });
});
