"use client";

import Link from "next/link";
import { ChevronLeft, Loader2 } from "lucide-react";
import { ProductDetail } from "@/components/product/ProductDetail";
import { useProduct } from "@/hooks/product/use-products";

// ========================================
// 상품 상세 페이지 (클라이언트 컴포넌트)
// ========================================

interface ProductDetailPageProps {
  id: string;
}

export function ProductDetailPage({ id }: ProductDetailPageProps) {
  const { data: product, isLoading, isError } = useProduct(id);

  if (isLoading) {
    return (
      <div
        className="flex items-center justify-center py-32"
        data-testid="product-detail-loading"
      >
        <Loader2 className="size-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (isError || !product) {
    return (
      <div
        className="mx-auto max-w-4xl px-4 py-16 text-center"
        data-testid="product-detail-error"
      >
        <Link
          href="/products"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-8"
        >
          <ChevronLeft className="size-4" />
          목록으로
        </Link>
        <p className="text-4xl mb-3">상품을 찾을 수 없습니다</p>
        <p className="text-sm text-muted-foreground">
          존재하지 않거나 삭제된 상품입니다.
        </p>
      </div>
    );
  }

  return <ProductDetail product={product} />;
}
