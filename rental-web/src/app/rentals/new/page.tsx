"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { RentalRequestForm } from "@/components/rental/RentalRequestForm";
import { getProductByIdApi } from "@/lib/api/product";
import type { Product } from "@/lib/api/types";

// ========================================
// 대여 신청 페이지
// 경로: /rentals/new?productId={id}
// ========================================

function RentalNewPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const productId = searchParams.get("productId");

  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!productId) {
      router.replace("/");
      return;
    }

    const load = async () => {
      try {
        setIsLoading(true);
        const res = await getProductByIdApi(productId);
        setProduct(res.data);
      } catch {
        setError("상품 정보를 불러올 수 없습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    load();
  }, [productId, router]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-muted-foreground">불러오는 중...</p>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4">
        <p className="text-destructive">{error ?? "상품을 찾을 수 없습니다."}</p>
        <Link href="/" className="text-primary underline text-sm">
          홈으로
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto px-4 pb-8">
      {/* 헤더 */}
      <div className="flex items-center gap-2 py-4 sticky top-0 bg-background z-10">
        <Link href={`/products/${product.id}`} aria-label="뒤로가기">
          <ChevronLeft className="size-5" />
        </Link>
        <h1 className="text-lg font-semibold flex-1 text-center pr-5">
          대여 신청
        </h1>
      </div>

      {/* 상품 미리보기 */}
      <div className="flex items-center gap-3 p-3 rounded-lg border mb-6 bg-muted/20">
        <div className="w-16 h-16 rounded-md overflow-hidden bg-muted shrink-0">
          {product.images[0]?.objectKey ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={product.images[0].objectKey}
              alt={product.name ?? "상품"}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
              없음
            </div>
          )}
        </div>
        <div>
          <p className="font-semibold text-sm">{product.name ?? "상품명 없음"}</p>
          <p className="text-xs text-muted-foreground mt-0.5">
            {product.categoryCode ?? "기타"}
          </p>
        </div>
      </div>

      {/* 대여 신청 폼 */}
      <RentalRequestForm product={product} />
    </div>
  );
}

export default function RentalNewPage() {
  return (
    <Suspense
      fallback={
        <div className="flex items-center justify-center min-h-screen">
          <p className="text-muted-foreground">불러오는 중...</p>
        </div>
      }
    >
      <RentalNewPageContent />
    </Suspense>
  );
}
