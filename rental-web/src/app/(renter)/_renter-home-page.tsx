"use client";

import { useMemo } from "react";
import { RenterHome } from "@/components/renter/RenterHome";
import { useProducts } from "@/hooks/product/use-products";

// ========================================
// 대여자 홈 페이지 (클라이언트 컴포넌트)
// ========================================

export function RenterHomePage() {
  const { data, isLoading } = useProducts({ size: 6 });

  const featuredProducts = useMemo(
    () => data?.pages.flatMap((p) => p.content) ?? [],
    [data]
  );

  return <RenterHome featuredProducts={featuredProducts} isLoading={isLoading} />;
}
