"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { getMyProductsApi } from "@/lib/api/draft";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
} from "@/components/ui/card";
import type { MyProductStatus } from "@/lib/api/types";

// ========================================
// 상태 필터 목록
// ========================================

const STATUS_FILTERS: { value: MyProductStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "DRAFT", label: "임시저장" },
  { value: "SUBMITTED", label: "검수중" },
  { value: "APPROVED", label: "승인완료" },
  { value: "REJECTED", label: "반려" },
  { value: "AVAILABLE", label: "대여가능" },
  { value: "RENTED", label: "대여중" },
  { value: "UNAVAILABLE", label: "비활성" },
];

// ========================================
// 상태별 Badge 색상
// ========================================

function getStatusBadgeVariant(
  status: MyProductStatus
): "default" | "secondary" | "outline" | "success" | "warning" | "destructive" | "muted" {
  switch (status) {
    case "AVAILABLE":
      return "success";
    case "RENTED":
      return "default";
    case "SUBMITTED":
      return "warning";
    case "APPROVED":
      return "success";
    case "REJECTED":
      return "destructive";
    case "DRAFT":
      return "muted";
    case "UNAVAILABLE":
      return "outline";
    default:
      return "secondary";
  }
}

function getStatusLabel(status: MyProductStatus): string {
  const found = STATUS_FILTERS.find((f) => f.value === status);
  return found ? found.label : status;
}

const CATEGORY_LABELS: Record<string, string> = {
  ELECTRONICS: "전자기기",
  FURNITURE: "가구/인테리어",
  SPORTS: "스포츠/레저",
  FASHION: "패션/의류",
  BOOKS: "도서/교재",
  TOOLS: "공구/DIY",
  VEHICLES: "이동수단",
  OTHERS: "기타",
};

// ========================================
// MyProductList 컴포넌트
// ========================================

export function MyProductList() {
  const [activeFilter, setActiveFilter] = useState<MyProductStatus | "ALL">("ALL");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["my-products", activeFilter],
    queryFn: () =>
      getMyProductsApi(
        activeFilter !== "ALL" ? { status: activeFilter } : undefined
      ),
  });

  const products = data?.data.content ?? [];

  return (
    <div className="space-y-4">
      {/* 상태 필터 탭 */}
      <div className="flex flex-wrap gap-2">
        {STATUS_FILTERS.map(({ value, label }) => (
          <button
            key={value}
            onClick={() => setActiveFilter(value)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
              activeFilter === value
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* 로딩 */}
      {isLoading && (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="h-24 animate-pulse rounded-xl bg-muted"
            />
          ))}
        </div>
      )}

      {/* 에러 */}
      {isError && (
        <p className="text-sm text-destructive">
          상품 목록을 불러오지 못했습니다.
        </p>
      )}

      {/* 상품 없음 */}
      {!isLoading && !isError && products.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">등록된 상품이 없습니다.</p>
          <Link href="/lender/products/new">
            <Button size="sm" className="mt-4">
              첫 상품 등록하기
            </Button>
          </Link>
        </div>
      )}

      {/* 상품 목록 */}
      {!isLoading && !isError && products.length > 0 && (
        <div className="space-y-3">
          {products.map((product) => (
            <Card key={product.id} size="sm">
              <CardContent className="flex items-center gap-4">
                {/* 썸네일 */}
                <div className="h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-muted">
                  {product.imageUrls[0] ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={product.imageUrls[0]}
                      alt={product.title}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center text-muted-foreground">
                      <svg
                        className="h-6 w-6"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={1.5}
                          d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                        />
                      </svg>
                    </div>
                  )}
                </div>

                {/* 정보 */}
                <div className="flex flex-1 flex-col gap-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <Badge variant={getStatusBadgeVariant(product.status)}>
                      {getStatusLabel(product.status)}
                    </Badge>
                    <span className="text-xs text-muted-foreground">
                      {CATEGORY_LABELS[product.category] ?? product.category}
                    </span>
                  </div>
                  <p className="truncate text-sm font-medium">{product.title}</p>
                  <p className="text-xs text-muted-foreground">
                    일 {product.pricePerDay.toLocaleString("ko-KR")}원 · 보증금{" "}
                    {product.deposit.toLocaleString("ko-KR")}원
                  </p>
                </div>

                {/* 액션 */}
                <div className="shrink-0">
                  {(product.status === "DRAFT" ||
                    product.status === "REJECTED") && (
                    <Link href={`/lender/products/${product.id}/edit`}>
                      <Button variant="outline" size="sm">
                        수정
                      </Button>
                    </Link>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
