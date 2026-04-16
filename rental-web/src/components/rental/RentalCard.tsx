"use client";

import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { Rental, RentalStatus } from "@/lib/api/types";

// ========================================
// 대여 상태 뱃지 설정
// ========================================

const STATUS_CONFIG: Record<RentalStatus, { label: string; className: string }> = {
  REQUESTED: {
    label: "대기 중",
    className: "bg-gray-100 text-gray-600",
  },
  APPROVED: {
    label: "승인됨",
    className: "bg-blue-100 text-blue-700",
  },
  PAID: {
    label: "결제 완료",
    className: "bg-green-100 text-green-700",
  },
  IN_USE: {
    label: "대여 중",
    className: "bg-orange-100 text-orange-700",
  },
  RETURNED: {
    label: "반납 완료",
    className: "bg-gray-200 text-gray-700",
  },
  CANCELLED: {
    label: "취소됨",
    className: "bg-red-100 text-red-700",
  },
};

// ========================================
// RentalCard 컴포넌트
// ========================================

interface RentalCardProps {
  rental: Rental;
  className?: string;
}

export function RentalCard({ rental, className }: RentalCardProps) {
  const statusConfig = STATUS_CONFIG[rental.status] ?? STATUS_CONFIG.REQUESTED;
  const startDate = rental.startDate.slice(0, 10);
  const endDate = rental.endDate.slice(5, 10);

  return (
    <Link href={`/rentals/${rental.id}`} className="block">
      <Card
        data-testid="rental-card"
        className={cn(
          "transition-shadow hover:shadow-md cursor-pointer",
          className
        )}
      >
        <CardContent className="flex gap-4 p-4">
          {/* 상품 이미지 */}
          <div className="relative w-16 h-16 shrink-0 rounded-md overflow-hidden bg-muted">
            {rental.productImageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={rental.productImageUrl}
                alt={rental.productName}
                className="h-full w-full object-cover"
                onError={(e) => {
                  (e.target as HTMLImageElement).src =
                    "/images/placeholder-product.svg";
                }}
              />
            ) : (
              <div className="flex h-full items-center justify-center text-muted-foreground text-xs">
                없음
              </div>
            )}
          </div>

          {/* 대여 정보 */}
          <div className="flex-1 min-w-0">
            <p
              data-testid="rental-card-product-name"
              className="font-semibold text-sm truncate"
            >
              {rental.productName}
            </p>
            <p className="text-xs text-muted-foreground mt-0.5">
              {startDate} ~ {endDate}
            </p>
            <div className="flex items-center justify-between mt-1.5">
              <span
                data-testid="rental-card-status"
                className={cn(
                  "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
                  statusConfig.className
                )}
              >
                {statusConfig.label}
              </span>
              <span className="text-sm font-semibold text-primary">
                {rental.totalAmount.toLocaleString()}원
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
