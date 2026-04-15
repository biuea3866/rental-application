"use client";

import { useEffect, useRef } from "react";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ProductCard } from "./ProductCard";
import type { ProductSummary } from "@/lib/api/types";

// ========================================
// ProductGrid 컴포넌트
// ========================================

interface ProductGridProps {
  products: ProductSummary[];
  isLoading?: boolean;
  isFetchingNextPage?: boolean;
  hasNextPage?: boolean;
  onLoadMore?: () => void;
  useInfiniteScroll?: boolean;
}

export function ProductGrid({
  products,
  isLoading = false,
  isFetchingNextPage = false,
  hasNextPage = false,
  onLoadMore,
  useInfiniteScroll = false,
}: ProductGridProps) {
  const loadMoreRef = useRef<HTMLDivElement>(null);

  // 무한 스크롤 IntersectionObserver
  useEffect(() => {
    if (!useInfiniteScroll || !hasNextPage || !onLoadMore) return;

    const el = loadMoreRef.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !isFetchingNextPage) {
          onLoadMore();
        }
      },
      { threshold: 0.5 }
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, [useInfiniteScroll, hasNextPage, onLoadMore, isFetchingNextPage]);

  // 로딩 스켈레톤
  if (isLoading) {
    return (
      <div
        className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4"
        data-testid="product-grid-skeleton"
      >
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="h-64 animate-pulse rounded-xl bg-muted"
            data-testid="product-skeleton"
          />
        ))}
      </div>
    );
  }

  // 빈 상태
  if (products.length === 0) {
    return (
      <div
        className="flex flex-col items-center justify-center py-20 text-center"
        data-testid="product-grid-empty"
      >
        <p className="text-4xl mb-4">검색 결과가 없습니다</p>
        <p className="text-muted-foreground text-sm">
          다른 검색어나 필터를 사용해 보세요.
        </p>
      </div>
    );
  }

  return (
    <div data-testid="product-grid">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      {/* 다음 페이지 로드 */}
      {useInfiniteScroll ? (
        <div ref={loadMoreRef} className="flex justify-center py-6">
          {isFetchingNextPage && (
            <Loader2
              className="size-6 animate-spin text-muted-foreground"
              data-testid="loading-spinner"
            />
          )}
        </div>
      ) : (
        hasNextPage && (
          <div className="flex justify-center pt-6">
            <Button
              variant="outline"
              onClick={onLoadMore}
              disabled={isFetchingNextPage}
              data-testid="load-more-button"
            >
              {isFetchingNextPage ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  불러오는 중...
                </>
              ) : (
                "더 보기"
              )}
            </Button>
          </div>
        )
      )}
    </div>
  );
}
