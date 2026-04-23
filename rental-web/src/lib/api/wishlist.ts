import { getApiClient, ApiRequestError } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { WishlistResult, WishlistPageResult } from "@/lib/api/types";

// ========================================
// 위시리스트 API Facade
// BFF 계층: Controller → Facade(이 파일) → ApiClient
// ========================================

export interface WishlistListParams {
  page?: number;
  size?: number;
}

/**
 * 위시리스트 목록 조회
 * GET /api/v1/wishlist?page=0&size=20
 */
export async function fetchWishlist(
  params: WishlistListParams = {}
): Promise<WishlistPageResult> {
  const client = getApiClient();
  const { page = 0, size = 20 } = params;

  const response = await client.get<WishlistPageResult>(
    ENDPOINTS.WISHLIST.BASE,
    {
      params: {
        page: String(page),
        size: String(size),
      },
    }
  );

  return response.data;
}

/**
 * 위시리스트 추가
 * POST /api/v1/wishlist/{productId} — 201 + WishlistResult, 409 if duplicate
 */
export async function addWishlist(productId: number): Promise<WishlistResult> {
  const client = getApiClient();

  const response = await client.post<WishlistResult>(
    ENDPOINTS.WISHLIST.BY_PRODUCT(productId)
  );

  return response.data;
}

/**
 * 위시리스트 삭제
 * DELETE /api/v1/wishlist/{productId} — 204, 404 if missing
 */
export async function removeWishlist(productId: number): Promise<void> {
  const client = getApiClient();

  await client.delete<void>(ENDPOINTS.WISHLIST.BY_PRODUCT(productId));
}

/**
 * 409 중복 에러 여부 판별
 */
export function isDuplicateWishlistError(error: unknown): boolean {
  return error instanceof ApiRequestError && error.status === 409;
}

/**
 * 404 미존재 에러 여부 판별
 */
export function isNotFoundWishlistError(error: unknown): boolean {
  return error instanceof ApiRequestError && error.status === 404;
}
