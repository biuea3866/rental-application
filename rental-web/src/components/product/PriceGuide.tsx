"use client";

import { useGuidePrices } from "@/hooks/product/use-products";
import { cn } from "@/lib/utils";
import type { ProductCategory } from "@/lib/api/types";

// ========================================
// 카테고리 한글 이름
// ========================================

const CATEGORY_LABELS: Record<ProductCategory, string> = {
  ELECTRONICS: "전자기기",
  FURNITURE: "가구",
  SPORTS: "스포츠",
  FASHION: "패션",
  BOOKS: "도서",
  TOOLS: "공구",
  VEHICLES: "이동수단",
  OTHERS: "기타",
};

// ========================================
// PriceGuide 컴포넌트
// ========================================

interface PriceGuideProps {
  highlightCategory?: ProductCategory;
  className?: string;
}

export function PriceGuide({ highlightCategory, className }: PriceGuideProps) {
  const { data: guidePrices, isLoading } = useGuidePrices();

  if (isLoading) {
    return (
      <div className={cn("space-y-2", className)} data-testid="price-guide-skeleton">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-10 animate-pulse rounded bg-muted" />
        ))}
      </div>
    );
  }

  if (!guidePrices || guidePrices.length === 0) {
    return null;
  }

  return (
    <div className={cn("space-y-1", className)} data-testid="price-guide">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
        카테고리별 가이드 가격
      </p>
      <div className="divide-y rounded-lg border overflow-hidden">
        {guidePrices.map((guide) => {
          const label = CATEGORY_LABELS[guide.category];
          const isHighlighted = guide.category === highlightCategory;

          return (
            <div
              key={guide.category}
              data-testid={`price-guide-${guide.category}`}
              className={cn(
                "flex items-center justify-between px-3 py-2 text-sm",
                isHighlighted && "bg-primary/5 font-medium"
              )}
            >
              <span className="text-muted-foreground">{label}</span>
              <div className="text-right">
                <span className="font-semibold">
                  {guide.averagePricePerDay.toLocaleString()}원
                </span>
                <span className="text-xs text-muted-foreground ml-1">/일 평균</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
