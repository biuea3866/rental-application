import { http, HttpResponse, delay } from "msw";
import type {
  ReviewResponse,
  CreateReviewRequest,
} from "@/lib/api/types";

// ========================================
// Review MSW 핸들러
// RC-FE-317
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ========================================
// 스텁 데이터
// ========================================

export const STUB_REVIEWS: ReviewResponse[] = [
  {
    reviewId: 1,
    renterId: 11,
    rentalId: 1001,
    productId: 42,
    rating: 5,
    content: "정말 좋은 제품이었습니다. 상태도 깨끗하고 대여 경험이 매우 만족스러웠어요.",
    createdAt: "2026-04-10T14:00:00+09:00",
  },
  {
    reviewId: 2,
    renterId: 11,
    rentalId: 1002,
    productId: 43,
    rating: 4,
    content: "카메라 상태가 좋았습니다. 다음에도 이용할 의향이 있습니다.",
    createdAt: "2026-04-08T10:30:00+09:00",
  },
  {
    reviewId: 3,
    renterId: 11,
    rentalId: 1003,
    productId: 44,
    rating: 3,
    content: "보통이었습니다. 제품 설명과 실제 상태가 약간 다른 점이 아쉬웠습니다.",
    createdAt: "2026-04-05T09:00:00+09:00",
  },
];

let stubReviews: ReviewResponse[] = [...STUB_REVIEWS];
let nextReviewId = 4;

export function resetReviewHandlerState(): void {
  stubReviews = [...STUB_REVIEWS];
  nextReviewId = 4;
}

// ========================================
// MSW 핸들러
// ========================================

export const reviewHandlers = [
  // 리뷰 작성 — POST /api/v1/reviews
  http.post(`${BASE_URL}/api/v1/reviews`, async ({ request }) => {
    await delay(300);
    const body = (await request.json()) as CreateReviewRequest;

    if (body.content.length < 10 || body.content.length > 500) {
      return HttpResponse.json(
        { code: "INVALID_CONTENT", message: "리뷰 내용은 10자 이상 500자 이하로 작성해주세요." },
        { status: 400 }
      );
    }

    const newReview: ReviewResponse = {
      reviewId: nextReviewId++,
      renterId: 11,
      rentalId: body.rentalId,
      productId: body.productId,
      rating: body.rating,
      content: body.content,
      createdAt: new Date().toISOString(),
    };

    stubReviews = [newReview, ...stubReviews];

    return HttpResponse.json(
      {
        success: true,
        data: newReview,
        timestamp: new Date().toISOString(),
      },
      { status: 201 }
    );
  }),

  // 상품 리뷰 목록 — GET /api/v1/products/:productId/reviews
  http.get(
    `${BASE_URL}/api/v1/products/:productId/reviews`,
    async ({ params, request }) => {
      await delay(200);
      const productId = Number(params.productId);
      const url = new URL(request.url);
      const page = parseInt(url.searchParams.get("page") ?? "0", 10);
      const size = parseInt(url.searchParams.get("size") ?? "10", 10);

      const filtered = stubReviews.filter((r) => r.productId === productId);
      const totalElements = filtered.length;
      const totalPages = Math.ceil(totalElements / size) || 1;
      const start = page * size;
      const content = filtered.slice(start, start + size);

      return HttpResponse.json({
        success: true,
        data: { content, totalElements, totalPages },
        timestamp: new Date().toISOString(),
      });
    }
  ),

  // 내 리뷰 목록 — GET /api/v1/my/reviews
  http.get(`${BASE_URL}/api/v1/my/reviews`, async ({ request }) => {
    await delay(200);
    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "10", 10);

    const totalElements = stubReviews.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const start = page * size;
    const content = stubReviews.slice(start, start + size);

    return HttpResponse.json({
      success: true,
      data: { content, totalElements, totalPages },
      timestamp: new Date().toISOString(),
    });
  }),
];
