"use client";

import {
  useQuery,
  useInfiniteQuery,
  type InfiniteData,
} from "@tanstack/react-query";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type {
  Product,
  ProductSummary,
  GuidePriceRange,
  ProductCategory,
} from "@/lib/api/types";

// ========================================
// Spring Page 응답 타입 (BE Page<ProductSummaryResponse>)
// ========================================

/** BE Spring Page 구조 */
interface SpringPage<T> {
  content: T[];
  pageable?: unknown;
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  numberOfElements?: number;
  empty?: boolean;
}

/** SpringPage → FE PaginatedResponse 변환 헬퍼 */
function toPageResult<T>(page: SpringPage<T>) {
  return {
    content: page.content,
    page: page.number,
    size: page.size,
    totalElements: page.totalElements,
    totalPages: page.totalPages,
    last: page.last,
    hasNext: !page.last,
  };
}

// ========================================
// 상품 목록 조회 (무한 스크롤) — BE ProductSummaryResponse 기준
// ========================================

export interface ProductSearchParams {
  keyword?: string;
  category?: ProductCategory;
  minPrice?: number;
  maxPrice?: number;
  /** BE sortBy: CREATED_AT | PRICE_ASC | PRICE_DESC */
  sort?: "latest" | "price_asc" | "price_desc";
  size?: number;
}

/** FE sort 파라미터 → BE sortBy 변환 */
function toBeSort(sort?: string): string {
  switch (sort) {
    case "price_asc":
      return "PRICE_ASC";
    case "price_desc":
      return "PRICE_DESC";
    default:
      return "CREATED_AT";
  }
}

interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  hasNext: boolean;
}

export function useProducts(params: ProductSearchParams = {}) {
  const client = getApiClient();
  const { keyword, category, minPrice, maxPrice, sort, size = 12 } = params;

  return useInfiniteQuery<
    PageResult<ProductSummary>,
    Error,
    InfiniteData<PageResult<ProductSummary>>,
    (string | ProductSearchParams)[],
    number
  >({
    queryKey: ["products", params],
    queryFn: async ({ pageParam = 0 }) => {
      const queryParams: Record<string, string> = {
        page: String(pageParam),
        size: String(size),
        sortBy: toBeSort(sort),
      };
      if (keyword) queryParams.keyword = keyword;
      if (category) queryParams.category = category;
      if (minPrice !== undefined) queryParams.minPrice = String(minPrice);
      if (maxPrice !== undefined) queryParams.maxPrice = String(maxPrice);

      const response = await client.get<SpringPage<ProductSummary>>(
        ENDPOINTS.PRODUCTS.BASE,
        { params: queryParams }
      );
      return toPageResult(response.data);
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.page + 1 : undefined,
  });
}

// ========================================
// 상품 상세 조회 — BE ProductDetailResponse 기준
// ========================================

export function useProduct(id: string) {
  const client = getApiClient();

  return useQuery<Product, Error>({
    queryKey: ["product", id],
    queryFn: async () => {
      const response = await client.get<Product>(ENDPOINTS.PRODUCTS.BY_ID(id));
      return response.data;
    },
    enabled: Boolean(id),
  });
}

// ========================================
// 가이드 가격 전체 조회 (BE: /api/v1/price-guides)
// ========================================

export function useGuidePrices() {
  const client = getApiClient();

  return useQuery<GuidePriceRange[], Error>({
    queryKey: ["guide-prices"],
    queryFn: async () => {
      const response = await client.get<GuidePriceRange[]>(
        ENDPOINTS.GUIDE_PRICES.BASE
      );
      return response.data;
    },
  });
}

// ========================================
// 카테고리별 가이드 가격 조회
// ========================================

export function useGuidePriceByCategory(category: ProductCategory | undefined) {
  const client = getApiClient();

  return useQuery<GuidePriceRange, Error>({
    queryKey: ["guide-price", category],
    queryFn: async () => {
      const response = await client.get<GuidePriceRange>(
        ENDPOINTS.GUIDE_PRICES.BY_CATEGORY(category!)
      );
      return response.data;
    },
    enabled: Boolean(category),
  });
}
