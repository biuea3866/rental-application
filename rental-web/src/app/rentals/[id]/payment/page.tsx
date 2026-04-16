"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { PaymentWidget } from "@/components/rental/PaymentWidget";
import { getRentalDetailApi } from "@/lib/api/rental";
import type { Rental } from "@/lib/api/types";

// ========================================
// 결제 페이지
// 경로: /rentals/{id}/payment
// ========================================

export default function RentalPaymentPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const [rental, setRental] = useState<Rental | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setIsLoading(true);
        const res = await getRentalDetailApi(id);
        if (res.data.status !== "APPROVED") {
          router.replace(`/rentals/${id}`);
          return;
        }
        setRental(res.data);
      } catch {
        setError("대여 정보를 불러올 수 없습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    load();
  }, [id, router]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-muted-foreground">불러오는 중...</p>
      </div>
    );
  }

  if (error || !rental) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4">
        <p className="text-destructive">
          {error ?? "결제 가능한 대여 정보가 없습니다."}
        </p>
        <Link href="/my-rentals" className="text-primary underline text-sm">
          내 대여 목록으로
        </Link>
      </div>
    );
  }

  const startDateStr = rental.startDate.slice(0, 10);
  const endDateStr = rental.endDate.slice(0, 10);

  return (
    <div className="max-w-lg mx-auto px-4 pb-8">
      {/* 헤더 */}
      <div className="flex items-center gap-2 py-4 sticky top-0 bg-background z-10">
        <Link href={`/rentals/${id}`} aria-label="뒤로가기">
          <ChevronLeft className="size-5" />
        </Link>
        <h1 className="text-lg font-semibold flex-1 text-center pr-5">결제</h1>
      </div>

      {/* 주문 정보 */}
      <section className="mb-4 p-4 rounded-lg border space-y-2">
        <h2 className="font-semibold">주문 정보</h2>
        <div className="text-sm space-y-1.5">
          <div className="flex justify-between">
            <span className="text-muted-foreground">상품명</span>
            <span className="font-medium">{rental.productName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">대여 기간</span>
            <span>
              {startDateStr} ~ {endDateStr}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">배송지</span>
            <span className="text-right max-w-[55%]">
              {rental.deliveryInfo.address}
            </span>
          </div>
        </div>
      </section>

      {/* Toss Payments 결제 위젯 */}
      <section className="mb-4">
        <PaymentWidget
          rental={rental}
          onSuccess={(rentalId) => router.push(`/rentals/${rentalId}`)}
        />
      </section>

      {/* 최종 결제 금액 */}
      <section className="p-4 rounded-lg border space-y-2 bg-muted/30">
        <h2 className="font-semibold">최종 결제 금액</h2>
        <div className="text-sm space-y-1.5">
          <div className="flex justify-between">
            <span className="text-muted-foreground">대여료</span>
            <span>
              {(rental.totalAmount - rental.depositAmount).toLocaleString()}원
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">보증금</span>
            <span>{rental.depositAmount.toLocaleString()}원</span>
          </div>
          <hr />
          <div className="flex justify-between font-semibold">
            <span>합계</span>
            <span className="text-primary">
              {rental.totalAmount.toLocaleString()}원
            </span>
          </div>
        </div>
      </section>
    </div>
  );
}
