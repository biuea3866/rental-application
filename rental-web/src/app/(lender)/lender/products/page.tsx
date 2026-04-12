"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { Product, PaginatedResponse } from "@/lib/api/types";

// ========================================
// 상태 레이블
// ========================================

const STATUS_LABELS: Record<Product["status"], string> = {
  AVAILABLE: "대여 가능",
  RENTED: "대여 중",
  UNAVAILABLE: "비활성",
};

const STATUS_COLORS: Record<Product["status"], string> = {
  AVAILABLE: "text-green-600 bg-green-50",
  RENTED: "text-blue-600 bg-blue-50",
  UNAVAILABLE: "text-gray-500 bg-gray-100",
};

const linkButtonClass =
  "inline-flex items-center justify-center rounded-lg border border-transparent bg-primary px-2.5 py-1 text-sm font-medium text-primary-foreground hover:bg-primary/80";

const linkOutlineButtonClass =
  "inline-flex items-center justify-center rounded-lg border border-border bg-background px-2.5 py-1 text-sm font-medium hover:bg-muted";

const linkSmOutlineButtonClass =
  "inline-flex items-center justify-center rounded-lg border border-border bg-background px-2 py-0.5 text-[0.8rem] font-medium hover:bg-muted";

// ========================================
// 등록자 내 상품 관리 페이지
// ========================================

export default function LenderProductsPage() {
  const apiClient = getApiClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["lender-products"],
    queryFn: async () => {
      const response = await apiClient.get<PaginatedResponse<Product>>(
        ENDPOINTS.PRODUCTS.MY_PRODUCTS
      );
      return response.data;
    },
  });

  const products = data?.content ?? [];

  return (
    <main className="flex flex-1 flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">내 등록 상품</h1>
        <Link href="/lender/products/new" className={linkButtonClass}>
          상품 등록
        </Link>
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <p className="text-sm text-muted-foreground">불러오는 중...</p>
        </div>
      )}

      {isError && (
        <div className="flex items-center justify-center py-12">
          <p className="text-sm text-destructive">
            상품을 불러오지 못했습니다. 다시 시도해주세요.
          </p>
        </div>
      )}

      {!isLoading && !isError && products.length === 0 && (
        <div className="flex flex-col items-center justify-center gap-4 py-16 text-center">
          <p className="text-muted-foreground">등록된 상품이 없습니다.</p>
          <Link href="/lender/products/new" className={linkOutlineButtonClass}>
            첫 상품 등록하기
          </Link>
        </div>
      )}

      {!isLoading && !isError && products.length > 0 && (
        <>
          {/* 요약 통계 */}
          <div className="grid grid-cols-3 gap-4">
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-4">
                <p className="text-2xl font-bold">{products.length}</p>
                <p className="text-sm text-muted-foreground">전체 상품</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-4">
                <p className="text-2xl font-bold text-green-600">
                  {products.filter((p) => p.status === "AVAILABLE").length}
                </p>
                <p className="text-sm text-muted-foreground">대여 가능</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-4">
                <p className="text-2xl font-bold text-blue-600">
                  {products.filter((p) => p.status === "RENTED").length}
                </p>
                <p className="text-sm text-muted-foreground">대여 중</p>
              </CardContent>
            </Card>
          </div>

          {/* 상품 목록 */}
          <div className="space-y-3">
            {products.map((product) => (
              <Card key={product.id}>
                <CardHeader className="flex flex-row items-start justify-between pb-2">
                  <div className="space-y-1">
                    <CardTitle className="text-base">{product.title}</CardTitle>
                    <p className="text-xs text-muted-foreground">
                      {product.category} · {product.location}
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      STATUS_COLORS[product.status]
                    }`}
                  >
                    {STATUS_LABELS[product.status]}
                  </span>
                </CardHeader>
                <CardContent className="flex items-center justify-between pt-0">
                  <div className="space-y-0.5">
                    <p className="text-sm font-medium">
                      {product.pricePerDay.toLocaleString("ko-KR")}원/일
                    </p>
                    <p className="text-xs text-muted-foreground">
                      보증금 {product.deposit.toLocaleString("ko-KR")}원
                    </p>
                  </div>
                  <Link
                    href={`/lender/products/${product.id}/edit`}
                    className={linkSmOutlineButtonClass}
                  >
                    수정
                  </Link>
                </CardContent>
              </Card>
            ))}
          </div>
        </>
      )}
    </main>
  );
}
