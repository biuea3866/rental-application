"use client";

import { useState } from "react";
import Link from "next/link";
import { MapPin, ChevronLeft, ChevronRight, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { PriceGuide } from "./PriceGuide";
import { cn } from "@/lib/utils";
import type { Product, ProductCategory, ProductStatus } from "@/lib/api/types";

// ========================================
// 상태 설정
// ========================================

const STATUS_CONFIG: Record<
  ProductStatus,
  { label: string; className: string }
> = {
  AVAILABLE: { label: "대여 가능", className: "bg-green-100 text-green-700" },
  RENTED: { label: "대여 중", className: "bg-red-100 text-red-700" },
  UNAVAILABLE: { label: "대여 불가", className: "bg-gray-100 text-gray-500" },
};

const CATEGORY_LABELS: Record<ProductCategory, string> = {
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
// 이미지 갤러리
// ========================================

interface ImageGalleryProps {
  images: string[];
  title: string;
}

function ImageGallery({ images, title }: ImageGalleryProps) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const prev = () =>
    setCurrentIndex((i) => (i - 1 + images.length) % images.length);
  const next = () =>
    setCurrentIndex((i) => (i + 1) % images.length);

  const src = images[currentIndex];

  return (
    <div className="space-y-2" data-testid="image-gallery">
      {/* 메인 이미지 */}
      <div className="relative aspect-[4/3] overflow-hidden rounded-xl bg-muted">
        {src ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={src}
            alt={`${title} - 이미지 ${currentIndex + 1}`}
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

        {/* 네비게이션 (이미지 2장 이상) */}
        {images.length > 1 && (
          <>
            <button
              type="button"
              onClick={prev}
              aria-label="이전 이미지"
              className="absolute left-2 top-1/2 -translate-y-1/2 flex items-center justify-center size-8 rounded-full bg-black/50 text-white hover:bg-black/70 transition-colors"
            >
              <ChevronLeft className="size-4" />
            </button>
            <button
              type="button"
              onClick={next}
              aria-label="다음 이미지"
              className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center justify-center size-8 rounded-full bg-black/50 text-white hover:bg-black/70 transition-colors"
            >
              <ChevronRight className="size-4" />
            </button>
            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
              {images.map((_, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => setCurrentIndex(i)}
                  aria-label={`이미지 ${i + 1}`}
                  className={cn(
                    "size-1.5 rounded-full transition-colors",
                    i === currentIndex ? "bg-white" : "bg-white/50"
                  )}
                />
              ))}
            </div>
          </>
        )}
      </div>

      {/* 썸네일 스트립 */}
      {images.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {images.map((img, i) => (
            <button
              key={i}
              type="button"
              onClick={() => setCurrentIndex(i)}
              className={cn(
                "relative shrink-0 aspect-square w-16 overflow-hidden rounded-lg border-2 transition-colors",
                i === currentIndex ? "border-primary" : "border-transparent"
              )}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={img}
                alt={`썸네일 ${i + 1}`}
                className="h-full w-full object-cover"
              />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ========================================
// ProductDetail 컴포넌트
// ========================================

interface ProductDetailProps {
  product: Product;
}

export function ProductDetail({ product }: ProductDetailProps) {
  const statusConfig = STATUS_CONFIG[product.status];
  const categoryLabel = CATEGORY_LABELS[product.category];
  const isAvailable = product.status === "AVAILABLE";

  return (
    <div className="mx-auto max-w-4xl px-4 py-6 space-y-6" data-testid="product-detail">
      {/* 뒤로가기 */}
      <Link
        href="/products"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        <ChevronLeft className="size-4" />
        목록으로
      </Link>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* 좌측: 이미지 + 설명 */}
        <div className="space-y-6">
          <ImageGallery images={product.imageUrls} title={product.title} />

          {/* 설명 */}
          <div>
            <h2 className="text-sm font-semibold text-muted-foreground mb-2">
              상품 설명
            </h2>
            <p className="text-sm leading-relaxed whitespace-pre-line text-foreground">
              {product.description}
            </p>
          </div>
        </div>

        {/* 우측: 상품 정보 + CTA */}
        <div className="space-y-4">
          {/* 카테고리 + 상태 */}
          <div className="flex items-center gap-2">
            <span className="rounded-full border px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
              {categoryLabel}
            </span>
            <span
              className={cn(
                "rounded-full px-2.5 py-0.5 text-xs font-medium",
                statusConfig.className
              )}
            >
              {statusConfig.label}
            </span>
          </div>

          {/* 제목 */}
          <h1 className="text-xl font-bold leading-snug">{product.title}</h1>

          {/* 가격 카드 */}
          <Card>
            <CardContent className="pt-4 space-y-3">
              <div className="flex items-baseline gap-1">
                <span className="text-2xl font-bold text-primary">
                  {product.pricePerDay.toLocaleString()}원
                </span>
                <span className="text-sm text-muted-foreground">/일</span>
              </div>
              <div className="text-sm text-muted-foreground">
                보증금{" "}
                <span className="font-semibold text-foreground">
                  {product.deposit.toLocaleString()}원
                </span>
              </div>

              <Button
                className="w-full"
                disabled={!isAvailable}
                data-testid="rental-request-button"
              >
                {isAvailable ? "대여 신청하기" : "대여 불가"}
              </Button>
            </CardContent>
          </Card>

          {/* 위치 */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <MapPin className="size-4 shrink-0" />
            <span>{product.location}</span>
          </div>

          {/* 등록자 정보 */}
          <div className="flex items-center gap-2 text-sm">
            <div className="flex items-center justify-center size-8 rounded-full bg-muted">
              <User className="size-4 text-muted-foreground" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">등록자</p>
              <p className="font-medium">{product.lenderName}</p>
            </div>
          </div>

          {/* 카테고리 가이드 가격 */}
          <PriceGuide highlightCategory={product.category} />
        </div>
      </div>
    </div>
  );
}
