"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { useQuery } from "@tanstack/react-query";
import { useRouter, usePathname } from "next/navigation";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import {
  conditionToApiParams,
  conditionToSearchParams,
  parseSearchParams,
} from "@/lib/api/product-search-types";
import type {
  ProductSearchCondition,
  SortBy,
  SortDirection,
  RegionCode,
} from "@/lib/api/product-search-types";
import type { ProductSummary } from "@/lib/api/types";

// ========================================
// Spring Page 타입
// ========================================

interface SpringPage<T> {
  content: T[];
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const DEBOUNCE_MS = 300;

// ========================================
// useProductSearch 훅
// ProductSearchCondition 기반 검색 + URL 동기화
// ========================================

export interface UseProductSearchOptions {
  initialCondition?: ProductSearchCondition;
}

export function useProductSearch(options: UseProductSearchOptions = {}) {
  const router = useRouter();
  const pathname = usePathname();

  const [condition, setCondition] = useState<ProductSearchCondition>({
    sortBy: "CREATED_AT",
    sortDirection: "DESC",
    page: 0,
    size: 20,
    ...options.initialCondition,
  });

  // debounce ref: 조건 변경 후 300ms 뒤 URL 업데이트
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const syncUrl = useCallback(
    (cond: ProductSearchCondition) => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(() => {
        const qs = conditionToSearchParams(cond).toString();
        router.push(`${pathname}${qs ? `?${qs}` : ""}`, { scroll: false });
      }, DEBOUNCE_MS);
    },
    [router, pathname]
  );

  // 검색 조건 업데이트 + URL 동기화
  const updateCondition = useCallback(
    (partial: Partial<ProductSearchCondition>) => {
      setCondition((prev) => {
        const next = { ...prev, ...partial, page: 0 };
        syncUrl(next);
        return next;
      });
    },
    [syncUrl]
  );

  // 페이지 변경
  const setPage = useCallback(
    (page: number) => {
      setCondition((prev) => {
        const next = { ...prev, page };
        syncUrl(next);
        return next;
      });
    },
    [syncUrl]
  );

  // 카테고리 변경
  const setCategoryCodes = useCallback(
    (codes: string[]) => {
      updateCondition({ categoryCodes: codes as ProductSearchCondition["categoryCodes"] });
    },
    [updateCondition]
  );

  // 가격 범위 변경
  const setPriceRange = useCallback(
    (min: number | undefined, max: number | undefined) => {
      updateCondition({ minPrice: min, maxPrice: max });
    },
    [updateCondition]
  );

  // 지역 변경
  const setRegion = useCallback(
    (region: RegionCode | undefined) => {
      updateCondition({ regionCode: region });
    },
    [updateCondition]
  );

  // 정렬 변경
  const setSort = useCallback(
    (sortBy: SortBy, sortDirection: SortDirection) => {
      updateCondition({ sortBy, sortDirection });
    },
    [updateCondition]
  );

  // 키워드 변경
  const setKeyword = useCallback(
    (keyword: string) => {
      updateCondition({ keyword: keyword || undefined });
    },
    [updateCondition]
  );

  // 전체 초기화
  const resetCondition = useCallback(() => {
    const reset: ProductSearchCondition = {
      sortBy: "CREATED_AT",
      sortDirection: "DESC",
      page: 0,
      size: 20,
    };
    setCondition(reset);
    syncUrl(reset);
  }, [syncUrl]);

  // BE API 호출
  const client = getApiClient();
  const apiParams = conditionToApiParams(condition);

  const query = useQuery<SpringPage<ProductSummary>, Error>({
    queryKey: ["products-search", condition],
    queryFn: async () => {
      const response = await client.get<SpringPage<ProductSummary>>(
        ENDPOINTS.PRODUCTS.BASE,
        { params: apiParams }
      );
      return response.data;
    },
    staleTime: 30_000,
  });

  const products = query.data?.content ?? [];
  const totalElements = query.data?.totalElements ?? 0;
  const totalPages = query.data?.totalPages ?? 0;
  const currentPage = query.data?.number ?? 0;
  const hasNextPage = query.data ? !query.data.last : false;
  const hasPrevPage = currentPage > 0;

  return {
    condition,
    products,
    totalElements,
    totalPages,
    currentPage,
    hasNextPage,
    hasPrevPage,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error: query.error,
    // 액션
    setKeyword,
    setCategoryCodes,
    setPriceRange,
    setRegion,
    setSort,
    setPage,
    resetCondition,
  };
}
