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
  PaginatedResponse,
  GuidePriceRange,
  ProductCategory,
} from "@/lib/api/types";

// ========================================
// 상품 목록 조회 (무한 스크롤)
// ========================================

export interface ProductSearchParams {
  keyword?: string;
  category?: ProductCategory;
  minPrice?: number;
  maxPrice?: number;
  sort?: "latest" | "price_asc" | "price_desc";
  size?: number;
}

export function useProducts(params: ProductSearchParams = {}) {
  const client = getApiClient();
  const { keyword, category, minPrice, maxPrice, sort, size = 12 } = params;

  return useInfiniteQuery<
    PaginatedResponse<Product>,
    Error,
    InfiniteData<PaginatedResponse<Product>>,
    (string | ProductSearchParams)[],
    number
  >({
    queryKey: ["products", params],
    queryFn: async ({ pageParam = 0 }) => {
      const queryParams: Record<string, string> = {
        page: String(pageParam),
        size: String(size),
      };
      if (keyword) queryParams.keyword = keyword;
      if (category) queryParams.category = category;
      if (minPrice !== undefined) queryParams.minPrice = String(minPrice);
      if (maxPrice !== undefined) queryParams.maxPrice = String(maxPrice);
      if (sort) queryParams.sort = sort;

      const response = await client.get<PaginatedResponse<Product>>(
        ENDPOINTS.PRODUCTS.BASE,
        { params: queryParams }
      );
      return response.data;
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.page + 1 : undefined,
  });
}

// ========================================
// 상품 상세 조회
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
// 가이드 가격 전체 조회
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
