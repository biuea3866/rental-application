"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { RentalRequestForm } from "@/components/rental/RentalRequestForm";
import type { Product } from "@/lib/api/types";

// ========================================
// 대여 신청 페이지 — /rentals/new?productId={id}
// ========================================

function RentalNewContent() {
  const searchParams = useSearchParams();
  const productId = searchParams.get("productId") ?? "";
  const client = getApiClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["product", productId],
    queryFn: async () => {
      const response = await client.get<Product>(
        ENDPOINTS.PRODUCTS.BY_ID(productId)
      );
      return response.data;
    },
    enabled: Boolean(productId),
  });

  if (isLoading) {
    return (
      <p className="text-sm text-muted-foreground">불러오는 중...</p>
    );
  }

  if (isError || !data) {
    return (
      <p className="text-sm text-destructive">
        상품을 불러오지 못했습니다. 다시 시도해주세요.
      </p>
    );
  }

  return (
    <>
      <h1 className="text-2xl font-bold">대여 신청</h1>

      {/* 상품 요약 */}
      <div className="flex items-center gap-3 rounded-lg border p-3">
        <div className="flex h-16 w-16 items-center justify-center rounded-md bg-muted text-xs text-muted-foreground">
          {data.images[0] ? (
            <img
              src={data.images[0].objectKey}
              alt={data.name ?? "상품 이미지"}
              className="h-full w-full rounded-md object-cover"
            />
          ) : (
            "이미지 없음"
          )}
        </div>
        <div>
          <p className="font-semibold">{data.name ?? "상품명 없음"}</p>
          <p className="text-xs text-muted-foreground">
            {data.categoryCode ?? ""} · {data.condition ?? ""}
          </p>
        </div>
      </div>

      <RentalRequestForm product={data} />
    </>
  );
}

export default function RentalNewPage() {
  return (
    <main className="flex flex-1 flex-col gap-4 p-6">
      <Suspense
        fallback={
          <p className="text-sm text-muted-foreground">불러오는 중...</p>
        }
      >
        <RentalNewContent />
      </Suspense>
    </main>
  );
}
