"use client";

import { useState } from "react";
import { Heart, ChevronLeft, ChevronRight } from "lucide-react";
import { useWishlistQuery } from "@/hooks/use-wishlist";
import { WishlistHeartButton } from "./WishlistHeartButton";
import { cn } from "@/lib/utils";

// ========================================
// WishlistTab — 마이페이지 위시리스트 탭
// ========================================

const PAGE_SIZE = 12;

export function WishlistTab() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useWishlistQuery(page, PAGE_SIZE);

  if (isLoading) {
    return (
      <section aria-label="위시리스트">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
          {Array.from({ length: PAGE_SIZE }).map((_, i) => (
            <div
              key={i}
              className="aspect-[4/3] animate-pulse rounded-lg bg-muted"
              aria-hidden="true"
            />
          ))}
        </div>
      </section>
    );
  }

  if (isError) {
    return (
      <section aria-label="위시리스트">
        <p className="text-sm text-destructive text-center py-12">
          위시리스트를 불러오지 못했습니다.
        </p>
      </section>
    );
  }

  const items = data?.items ?? [];
  const totalPages = data?.totalPages ?? 1;

  if (items.length === 0) {
    return (
      <section aria-label="위시리스트">
        <div className="flex flex-col items-center justify-center py-20 gap-4 text-center">
          <Heart className="size-12 text-muted-foreground/40" aria-hidden="true" />
          <p className="text-muted-foreground">위시리스트가 비어 있습니다.</p>
          <p className="text-sm text-muted-foreground/70">
            마음에 드는 상품에 하트를 눌러 저장하세요.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section aria-label="위시리스트">
      {/* 아이템 그리드 */}
      <ul
        className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4"
        aria-label={`위시리스트 ${items.length}개 항목`}
      >
        {items.map((item) => (
          <li
            key={item.id}
            className="relative rounded-lg border bg-card overflow-hidden"
          >
            {/* 썸네일 플레이스홀더 */}
            <div className="aspect-[4/3] bg-muted flex items-center justify-center text-xs text-muted-foreground">
              상품 #{item.productId}
            </div>

            {/* 하트 버튼 */}
            <div className="absolute top-2 right-2">
              <WishlistHeartButton
                productId={item.productId}
                isWished={true}
                size="sm"
                aria-label={`상품 ${item.productId} 위시리스트에서 제거`}
              />
            </div>

            <div className="p-3">
              <p className="text-sm font-medium truncate">
                상품 #{item.productId}
              </p>
              <p className="text-xs text-muted-foreground mt-0.5">
                {new Date(item.createdAt).toLocaleDateString("ko-KR")} 추가됨
              </p>
            </div>
          </li>
        ))}
      </ul>

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <nav
          className="flex items-center justify-center gap-2 mt-8"
          aria-label="위시리스트 페이지네이션"
        >
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            aria-label="이전 페이지"
            className={cn(
              "flex items-center justify-center rounded-md p-2 text-sm",
              "border transition-colors",
              "disabled:opacity-40 disabled:cursor-not-allowed",
              "hover:bg-muted enabled:cursor-pointer"
            )}
          >
            <ChevronLeft className="size-4" aria-hidden="true" />
          </button>

          {Array.from({ length: totalPages }).map((_, i) => (
            <button
              key={i}
              type="button"
              onClick={() => setPage(i)}
              aria-current={i === page ? "page" : undefined}
              aria-label={`${i + 1}페이지`}
              className={cn(
                "flex h-8 w-8 items-center justify-center rounded-md text-sm border transition-colors",
                i === page
                  ? "bg-primary text-primary-foreground border-primary font-semibold"
                  : "hover:bg-muted"
              )}
            >
              {i + 1}
            </button>
          ))}

          <button
            type="button"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            aria-label="다음 페이지"
            className={cn(
              "flex items-center justify-center rounded-md p-2 text-sm",
              "border transition-colors",
              "disabled:opacity-40 disabled:cursor-not-allowed",
              "hover:bg-muted enabled:cursor-pointer"
            )}
          >
            <ChevronRight className="size-4" aria-hidden="true" />
          </button>
        </nav>
      )}

      {/* 총 개수 */}
      <p className="text-xs text-muted-foreground text-center mt-4">
        총 {data?.totalElements ?? 0}개의 위시리스트 상품
      </p>
    </section>
  );
}
