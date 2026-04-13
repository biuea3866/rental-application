"use client";

import { useCallback } from "react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { ProductCategory } from "@/lib/api/types";

// ========================================
// 필터 설정
// ========================================

export const CATEGORY_OPTIONS: {
  value: ProductCategory | "";
  label: string;
  emoji: string;
}[] = [
  { value: "", label: "전체", emoji: "🔍" },
  { value: "ELECTRONICS", label: "전자기기", emoji: "💻" },
  { value: "FURNITURE", label: "가구", emoji: "🪑" },
  { value: "SPORTS", label: "스포츠", emoji: "⛺" },
  { value: "FASHION", label: "패션", emoji: "👜" },
  { value: "BOOKS", label: "도서", emoji: "📚" },
  { value: "TOOLS", label: "공구", emoji: "🔧" },
  { value: "VEHICLES", label: "이동수단", emoji: "🛴" },
  { value: "OTHERS", label: "기타", emoji: "📦" },
];

export const SORT_OPTIONS: { value: string; label: string }[] = [
  { value: "latest", label: "최신순" },
  { value: "price_asc", label: "가격 낮은순" },
  { value: "price_desc", label: "가격 높은순" },
];

const PRICE_PRESETS: { label: string; min: number; max: number }[] = [
  { label: "~1만원", min: 0, max: 10000 },
  { label: "1~3만원", min: 10000, max: 30000 },
  { label: "3~5만원", min: 30000, max: 50000 },
  { label: "5만원~", min: 50000, max: 9999999 },
];

// ========================================
// 필터 상태 타입
// ========================================

export interface FilterState {
  category: ProductCategory | "";
  minPrice: number | undefined;
  maxPrice: number | undefined;
  sort: string;
}

// ========================================
// ProductFilters 컴포넌트
// ========================================

interface ProductFiltersProps {
  filters: FilterState;
  onChange: (filters: FilterState) => void;
  className?: string;
}

export function ProductFilters({
  filters,
  onChange,
  className,
}: ProductFiltersProps) {
  const handleCategory = useCallback(
    (category: ProductCategory | "") => {
      onChange({ ...filters, category });
    },
    [filters, onChange]
  );

  const handleSort = useCallback(
    (sort: string) => {
      onChange({ ...filters, sort });
    },
    [filters, onChange]
  );

  const handlePricePreset = useCallback(
    (min: number, max: number) => {
      const isSame = filters.minPrice === min && filters.maxPrice === max;
      onChange({
        ...filters,
        minPrice: isSame ? undefined : min,
        maxPrice: isSame ? undefined : max,
      });
    },
    [filters, onChange]
  );

  const handleReset = useCallback(() => {
    onChange({
      category: "",
      minPrice: undefined,
      maxPrice: undefined,
      sort: "latest",
    });
  }, [onChange]);

  const hasActiveFilters =
    filters.category !== "" ||
    filters.minPrice !== undefined ||
    filters.maxPrice !== undefined ||
    filters.sort !== "latest";

  return (
    <div className={cn("space-y-4", className)} data-testid="product-filters">
      {/* 카테고리 */}
      <div>
        <p className="mb-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          카테고리
        </p>
        <div className="flex flex-wrap gap-1.5">
          {CATEGORY_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => handleCategory(opt.value)}
              data-testid={`category-filter-${opt.value || "all"}`}
              className={cn(
                "inline-flex items-center gap-1 rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                filters.category === opt.value
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-background hover:bg-muted"
              )}
            >
              <span>{opt.emoji}</span>
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 가격 범위 */}
      <div>
        <p className="mb-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          가격 (1일 기준)
        </p>
        <div className="flex flex-wrap gap-1.5">
          {PRICE_PRESETS.map((preset) => {
            const isActive =
              filters.minPrice === preset.min &&
              filters.maxPrice === preset.max;
            return (
              <button
                key={preset.label}
                type="button"
                onClick={() => handlePricePreset(preset.min, preset.max)}
                data-testid={`price-filter-${preset.label}`}
                className={cn(
                  "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                  isActive
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border bg-background hover:bg-muted"
                )}
              >
                {preset.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* 정렬 */}
      <div>
        <p className="mb-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          정렬
        </p>
        <div className="flex flex-wrap gap-1.5">
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => handleSort(opt.value)}
              data-testid={`sort-filter-${opt.value}`}
              className={cn(
                "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                filters.sort === opt.value
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-background hover:bg-muted"
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 필터 초기화 */}
      {hasActiveFilters && (
        <Button
          variant="ghost"
          size="sm"
          onClick={handleReset}
          className="text-xs text-muted-foreground"
          data-testid="filter-reset-button"
        >
          필터 초기화
        </Button>
      )}
    </div>
  );
}
