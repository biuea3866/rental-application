"use client";

import { useCallback, useState } from "react";
import { SlidersHorizontal, RotateCcw } from "lucide-react";
import { CategoryFilter } from "./CategoryFilter";
import { PriceRangeInput } from "./PriceRangeInput";
import { RegionDropdown } from "./RegionDropdown";
import { SortToggle } from "./SortToggle";
import { ProductSearchBar } from "@/components/product/ProductSearchBar";
import { ProductGrid } from "@/components/product/ProductGrid";
import { Pagination } from "./Pagination";
import { useProductSearch } from "@/hooks/product/use-product-search";
import { parseSearchParams } from "@/lib/api/product-search-types";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { SortBy, SortDirection, RegionCode } from "@/lib/api/product-search-types";
import type { ProductCategory } from "@/lib/api/types";

// ========================================
// ProductSearchPage — FE-452 검색 필터/정렬 통합 컴포넌트
// ========================================

interface ProductSearchPageProps {
  initialParams?: Record<string, string | string[] | undefined>;
  className?: string;
}

export function ProductSearchPage({
  initialParams = {},
  className,
}: ProductSearchPageProps) {
  const initialCondition = parseSearchParams(initialParams);

  const {
    condition,
    products,
    totalElements,
    totalPages,
    currentPage,
    hasNextPage,
    hasPrevPage,
    isLoading,
    isFetching,
    setKeyword,
    setCategoryCodes,
    setPriceRange,
    setRegion,
    setSort,
    setPage,
    resetCondition,
  } = useProductSearch({ initialCondition });

  const [showFilters, setShowFilters] = useState(false);

  const hasActiveFilters =
    (condition.categoryCodes?.length ?? 0) > 0 ||
    condition.regionCode !== undefined ||
    condition.minPrice !== undefined ||
    condition.maxPrice !== undefined ||
    condition.sortBy !== "CREATED_AT";

  const handleSortChange = useCallback(
    (value: { sortBy: SortBy; sortDirection: SortDirection }) => {
      setSort(value.sortBy, value.sortDirection);
    },
    [setSort]
  );

  const handleRegionChange = useCallback(
    (region: RegionCode | undefined) => {
      setRegion(region);
    },
    [setRegion]
  );

  const handleCategoryChange = useCallback(
    (codes: ProductCategory[]) => {
      setCategoryCodes(codes);
    },
    [setCategoryCodes]
  );

  const handlePriceChange = useCallback(
    (min: number | undefined, max: number | undefined) => {
      setPriceRange(min, max);
    },
    [setPriceRange]
  );

  return (
    <div className={cn("mx-auto max-w-4xl px-4 py-6 space-y-4", className)}>
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-bold">상품 검색</h1>
        {hasActiveFilters && (
          <Button
            variant="ghost"
            size="sm"
            onClick={resetCondition}
            className="gap-1.5 text-muted-foreground text-xs"
            data-testid="reset-filters-button"
          >
            <RotateCcw className="size-3.5" />
            초기화
          </Button>
        )}
      </div>

      {/* 검색바 */}
      <ProductSearchBar
        defaultValue={condition.keyword ?? ""}
        onSearch={setKeyword}
        data-testid="product-search-bar"
      />

      {/* 결과 수 + 필터 토글 */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground" data-testid="result-count">
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
          aria-expanded={showFilters}
          aria-controls="filter-panel"
          data-testid="toggle-filters-button"
        >
          <SlidersHorizontal className="size-3.5" />
          필터
          {hasActiveFilters && (
            <span
              className="ml-1 size-1.5 rounded-full bg-primary"
              aria-hidden="true"
            />
          )}
        </Button>
      </div>

      {/* 필터 패널 */}
      {showFilters && (
        <div
          id="filter-panel"
          className="rounded-xl border p-4 space-y-4"
          data-testid="filter-panel"
        >
          {/* 카테고리 */}
          <CategoryFilter
            selectedCodes={condition.categoryCodes ?? []}
            onChange={handleCategoryChange}
          />

          {/* 가격 범위 */}
          <PriceRangeInput
            minPrice={condition.minPrice}
            maxPrice={condition.maxPrice}
            onChange={handlePriceChange}
          />

          {/* 지역 */}
          <RegionDropdown
            value={condition.regionCode}
            onChange={handleRegionChange}
          />

          {/* 정렬 */}
          <SortToggle
            sortBy={condition.sortBy ?? "CREATED_AT"}
            sortDirection={condition.sortDirection ?? "DESC"}
            onChange={handleSortChange}
          />
        </div>
      )}

      {/* 필터 패널이 닫혀 있을 때도 정렬은 상단에 표시 */}
      {!showFilters && (
        <SortToggle
          sortBy={condition.sortBy ?? "CREATED_AT"}
          sortDirection={condition.sortDirection ?? "DESC"}
          onChange={handleSortChange}
        />
      )}

      {/* 상품 그리드 */}
      <ProductGrid
        products={products}
        isLoading={isLoading}
        isFetchingNextPage={isFetching && !isLoading}
      />

      {/* 페이지네이션 */}
      {!isLoading && totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          hasNext={hasNextPage}
          hasPrev={hasPrevPage}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
