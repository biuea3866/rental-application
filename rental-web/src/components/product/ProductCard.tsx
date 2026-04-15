"use client";

import Link from "next/link";
import { MapPin } from "lucide-react";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { ProductSummary, ProductStatus } from "@/lib/api/types";

// ========================================
// 상태 뱃지 설정 (BE ProductStatus enum 기준)
// ========================================

const STATUS_CONFIG: Record<
  ProductStatus,
  { label: string; className: string }
> = {
  DRAFT: { label: "초안", className: "bg-gray-100 text-gray-500" },
  UNDER_REVIEW: { label: "검토 중", className: "bg-yellow-100 text-yellow-700" },
  APPROVED: { label: "대여 가능", className: "bg-green-100 text-green-700" },
  REJECTED: { label: "반려", className: "bg-red-100 text-red-700" },
  SUSPENDED: { label: "정지", className: "bg-gray-100 text-gray-500" },
};

// ========================================
// 카테고리 한글 이름
// ========================================

const CATEGORY_LABELS: Record<string, string> = {
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
// ProductCard 컴포넌트 (ProductSummary 기준)
// ========================================

interface ProductCardProps {
  product: ProductSummary;
  className?: string;
}

export function ProductCard({ product, className }: ProductCardProps) {
  const statusConfig = STATUS_CONFIG[product.status] ?? STATUS_CONFIG.APPROVED;
  const categoryLabel = product.categoryCode
    ? (CATEGORY_LABELS[product.categoryCode] ?? product.categoryCode)
    : "기타";
  const isAvailable = product.status === "APPROVED";

  return (
    <Link href={`/products/${product.id}`} className="block h-full">
      <Card
        className={cn(
          "h-full transition-shadow hover:shadow-md cursor-pointer",
          !isAvailable && "opacity-70",
          className
        )}
      >
        {/* 이미지 영역 */}
        <div className="relative aspect-[4/3] overflow-hidden bg-muted">
          {product.thumbnailUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={product.thumbnailUrl}
              alt={product.name ?? "상품 이미지"}
              className="h-full w-full object-cover"
              onError={(e) => {
                (e.target as HTMLImageElement).src =
                  "/images/placeholder-product.svg";
              }}
            />
          ) : (
            <div className="flex h-full items-center justify-center text-muted-foreground text-sm">
              이미지 없음
            </div>
          )}

          {/* 상태 뱃지 */}
          <span
            className={cn(
              "absolute top-2 right-2 rounded-full px-2 py-0.5 text-xs font-medium",
              statusConfig.className
            )}
          >
            {statusConfig.label}
          </span>

          {/* 카테고리 뱃지 */}
          <span className="absolute top-2 left-2 rounded-full bg-black/60 px-2 py-0.5 text-xs text-white">
            {categoryLabel}
          </span>
        </div>

        {/* 본문 */}
        <CardContent className="pt-3 pb-0">
          <h3 className="line-clamp-2 text-sm font-semibold leading-snug">
            {product.name ?? "상품명 없음"}
          </h3>
        </CardContent>

        {/* 보증금 */}
        <CardFooter className="border-none bg-transparent pt-2 flex flex-col items-start gap-0.5">
          {product.depositAmount != null && (
            <div className="flex items-baseline gap-1">
              <span className="text-xs text-muted-foreground">보증금</span>
              <span className="text-sm font-semibold text-primary">
                {product.depositAmount.toLocaleString()}원
              </span>
            </div>
          )}
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <MapPin className="size-3 shrink-0" />
            <span className="truncate">위치 미제공</span>
          </div>
        </CardFooter>
      </Card>
    </Link>
  );
}
