"use client";

import { useCallback, useMemo, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { SlidersHorizontal } from "lucide-react";
import { ProductSearchBar } from "@/components/product/ProductSearchBar";
import { ProductFilters, type FilterState } from "@/components/product/ProductFilters";
import { ProductGrid } from "@/components/product/ProductGrid";
import { Button } from "@/components/ui/button";
import { useProducts } from "@/hooks/product/use-products";
import type { ProductCategory } from "@/lib/api/types";

// ========================================
// URL 파라미터 → 필터 상태 변환
// ========================================

interface InitialParams {
  keyword?: string;
  category?: string;
  minPrice?: string;
  maxPrice?: string;
  sort?: string;
}

function paramsToFilterState(params: InitialParams): FilterState {
  return {
    category: (params.category as ProductCategory) || "",
    minPrice: params.minPrice ? Number(params.minPrice) : undefined,
    maxPrice: params.maxPrice ? Number(params.maxPrice) : undefined,
    sort: params.sort || "latest",
  };
}

function filterStateToParams(
  keyword: string,
  filters: FilterState
): Record<string, string> {
  const params: Record<string, string> = {};
  if (keyword) params.keyword = keyword;
  if (filters.category) params.category = filters.category;
  if (filters.minPrice !== undefined) params.minPrice = String(filters.minPrice);
  if (filters.maxPrice !== undefined) params.maxPrice = String(filters.maxPrice);
  if (filters.sort && filters.sort !== "latest") params.sort = filters.sort;
  return params;
}

// ========================================
// 상품 목록 페이지 (클라이언트)
// ========================================

interface ProductsPageProps {
  initialParams: InitialParams;
}

export function ProductsPage({ initialParams }: ProductsPageProps) {
  const router = useRouter();
  const pathname = usePathname();

  const [keyword, setKeyword] = useState(initialParams.keyword ?? "");
  const [filters, setFilters] = useState<FilterState>(() =>
    paramsToFilterState(initialParams)
  );
  const [showFilters, setShowFilters] = useState(false);

  // URL 동기화
  const syncUrl = useCallback(
    (newKeyword: string, newFilters: FilterState) => {
      const params = filterStateToParams(newKeyword, newFilters);
      const qs = new URLSearchParams(params).toString();
      router.push(`${pathname}${qs ? `?${qs}` : ""}`);
    },
    [router, pathname]
  );

  const handleSearch = useCallback(
    (q: string) => {
      setKeyword(q);
      syncUrl(q, filters);
    },
    [filters, syncUrl]
  );

  const handleFilterChange = useCallback(
    (newFilters: FilterState) => {
      setFilters(newFilters);
      syncUrl(keyword, newFilters);
    },
    [keyword, syncUrl]
  );

  // 상품 조회
  const {
    data,
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useProducts({
    keyword: keyword || undefined,
    category: filters.category || undefined,
    minPrice: filters.minPrice,
    maxPrice: filters.maxPrice,
    sort: filters.sort as "latest" | "price_asc" | "price_desc",
    size: 12,
  });

  const products = useMemo(
    () => data?.pages.flatMap((p) => p.content) ?? [],
    [data]
  );

  const totalElements = data?.pages[0]?.totalElements ?? 0;

  return (
    <div className="mx-auto max-w-4xl px-4 py-6 space-y-4">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-bold">상품 검색</h1>
      </div>

      {/* 검색바 */}
      <ProductSearchBar
        defaultValue={keyword}
        onSearch={handleSearch}
        data-testid="products-search-bar"
      />

      {/* 필터 토글 (모바일) */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {!isLoading && (
            <>
              <span className="font-semibold text-foreground">
                {totalElements.toLocaleString()}
              </span>
              개의 상품
            </>
          )}
        </p>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setShowFilters((v) => !v)}
          className="gap-1.5"
          data-testid="toggle-filters-button"
        >
          <SlidersHorizontal className="size-3.5" />
          필터
          {(filters.category ||
            filters.minPrice !== undefined ||
            filters.sort !== "latest") && (
            <span className="ml-1 size-1.5 rounded-full bg-primary" />
          )}
        </Button>
      </div>

      {/* 필터 패널 */}
      {showFilters && (
        <div className="rounded-xl border p-4" data-testid="filter-panel">
          <ProductFilters filters={filters} onChange={handleFilterChange} />
        </div>
      )}

      {/* 상품 그리드 */}
      <ProductGrid
        products={products}
        isLoading={isLoading}
        isFetchingNextPage={isFetchingNextPage}
        hasNextPage={hasNextPage}
        onLoadMore={fetchNextPage}
        useInfiniteScroll={false}
      />
    </div>
  );
}
