"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ProductCard } from "@/components/product/ProductCard";
import { CATEGORY_OPTIONS } from "@/components/product/ProductFilters";
import { cn } from "@/lib/utils";
import type { Product } from "@/lib/api/types";

// ========================================
// 배너 컴포넌트
// ========================================

function HomeBanner() {
  return (
    <section
      className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary to-primary/80 px-6 py-10 text-primary-foreground"
      data-testid="home-banner"
    >
      <div className="relative z-10 max-w-md">
        <p className="text-sm font-medium opacity-80 mb-2">
          대여 커머스 플랫폼
        </p>
        <h1 className="text-2xl font-bold leading-tight mb-4">
          필요한 물건을
          <br />
          합리적으로 빌려보세요
        </h1>
        <p className="text-sm opacity-70 mb-6">
          전자기기, 가구, 스포츠 용품 등<br />
          다양한 상품을 일 단위로 대여할 수 있어요.
        </p>
        <Link
          href="/products"
          className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-secondary px-4 text-sm font-medium text-secondary-foreground hover:bg-secondary/80 transition-colors"
        >
          상품 둘러보기
          <ArrowRight className="size-4" />
        </Link>
      </div>

      {/* 장식 원 */}
      <div className="absolute -right-10 -top-10 size-48 rounded-full bg-white/10" />
      <div className="absolute -right-4 bottom-0 size-32 rounded-full bg-white/5" />
    </section>
  );
}

// ========================================
// 카테고리 그리드
// ========================================

function CategoryGrid() {
  const categories = CATEGORY_OPTIONS.filter((c) => c.value !== "");

  return (
    <section data-testid="category-grid">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-bold">카테고리</h2>
        <Link
          href="/products"
          className="text-xs text-muted-foreground hover:text-foreground transition-colors flex items-center gap-0.5"
        >
          전체보기 <ArrowRight className="size-3" />
        </Link>
      </div>

      <div className="grid grid-cols-4 gap-2 sm:grid-cols-8">
        {categories.map((cat) => (
          <Link
            key={cat.value}
            href={`/products?category=${cat.value}`}
            className={cn(
              "flex flex-col items-center gap-1.5 rounded-xl border p-3 text-center transition-colors hover:bg-muted"
            )}
            data-testid={`category-link-${cat.value}`}
          >
            <span className="text-2xl">{cat.emoji}</span>
            <span className="text-xs font-medium text-muted-foreground">
              {cat.label}
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}

// ========================================
// 추천 상품 섹션
// ========================================

interface FeaturedProductsProps {
  products: Product[];
  isLoading?: boolean;
}

function FeaturedProducts({ products, isLoading }: FeaturedProductsProps) {
  return (
    <section data-testid="featured-products">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-bold">추천 상품</h2>
        <Link
          href="/products"
          className="text-xs text-muted-foreground hover:text-foreground transition-colors flex items-center gap-0.5"
        >
          전체보기 <ArrowRight className="size-3" />
        </Link>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={i}
              className="h-56 animate-pulse rounded-xl bg-muted"
            />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {products.slice(0, 6).map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </section>
  );
}

// ========================================
// RenterHome 메인 컴포넌트
// ========================================

interface RenterHomeProps {
  featuredProducts?: Product[];
  isLoading?: boolean;
}

export function RenterHome({
  featuredProducts = [],
  isLoading = false,
}: RenterHomeProps) {
  return (
    <main className="mx-auto max-w-2xl px-4 py-6 space-y-8">
      <HomeBanner />
      <CategoryGrid />
      <FeaturedProducts products={featuredProducts} isLoading={isLoading} />
    </main>
  );
}
