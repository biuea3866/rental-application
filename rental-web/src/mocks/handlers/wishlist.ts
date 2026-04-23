import { http, HttpResponse, delay } from "msw";
import type { WishlistResult, WishlistPageResult } from "@/lib/api/types";
import { STUB_PRODUCTS } from "../products";

// ========================================
// 위시리스트 MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 인메모리 위시리스트 상태 (productId Set)
let wishlistItems: WishlistResult[] = [];
let nextId = 1;

export function resetWishlistHandlerState(): void {
  wishlistItems = [];
  nextId = 1;
}

/** 테스트용 초기 데이터 주입 */
export function seedWishlistItems(productIds: number[]): void {
  wishlistItems = productIds.map((productId) => ({
    id: nextId++,
    userId: 1,
    productId,
    createdAt: new Date().toISOString(),
  }));
}

export const wishlistHandlers = [
  // 위시리스트 목록 조회
  // GET /api/v1/wishlist?page=0&size=20
  http.get(`${BASE_URL}/api/v1/wishlist`, async ({ request }) => {
    await delay(100);

    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    const start = page * size;
    const items = wishlistItems.slice(start, start + size);
    const totalElements = wishlistItems.length;
    const totalPages = Math.max(1, Math.ceil(totalElements / size));

    const result: WishlistPageResult = {
      items,
      totalElements,
      totalPages,
    };

    return HttpResponse.json({
      success: true,
      data: result,
      timestamp: new Date().toISOString(),
    });
  }),

  // 위시리스트 추가
  // POST /api/v1/wishlist/:productId — 201 or 409
  http.post(`${BASE_URL}/api/v1/wishlist/:productId`, async ({ params }) => {
    await delay(100);

    const productId = Number(params.productId);
    const exists = wishlistItems.some((item) => item.productId === productId);

    if (exists) {
      return HttpResponse.json(
        { code: "WISHLIST_DUPLICATE", message: "이미 위시리스트에 추가된 상품입니다." },
        { status: 409 }
      );
    }

    // 상품 존재 여부 확인
    const product = STUB_PRODUCTS.find((p) => p.id === productId);
    if (!product) {
      return HttpResponse.json(
        { code: "PRODUCT_NOT_FOUND", message: "상품을 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    const newItem: WishlistResult = {
      id: nextId++,
      userId: 1,
      productId,
      createdAt: new Date().toISOString(),
    };
    wishlistItems.push(newItem);

    return HttpResponse.json(
      {
        success: true,
        data: newItem,
        timestamp: new Date().toISOString(),
      },
      { status: 201 }
    );
  }),

  // 위시리스트 삭제
  // DELETE /api/v1/wishlist/:productId — 204 or 404
  http.delete(`${BASE_URL}/api/v1/wishlist/:productId`, async ({ params }) => {
    await delay(100);

    const productId = Number(params.productId);
    const index = wishlistItems.findIndex((item) => item.productId === productId);

    if (index === -1) {
      return HttpResponse.json(
        { code: "WISHLIST_NOT_FOUND", message: "위시리스트에 없는 상품입니다." },
        { status: 404 }
      );
    }

    wishlistItems.splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),
];
