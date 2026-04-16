"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { Product, CreateRentalRequest } from "@/lib/api/types";

// ========================================
// 대여 신청 폼 컴포넌트
// ========================================

interface RentalRequestFormProps {
  product: Product;
}

export function RentalRequestForm({ product }: RentalRequestFormProps) {
  const router = useRouter();
  const client = getApiClient();

  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [addressLine1, setAddressLine1] = useState("");
  const [addressLine2, setAddressLine2] = useState("");
  const [zipCode, setZipCode] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /** 대여 일수 계산 */
  function calcDays(): number {
    if (!startDate || !endDate) return 0;
    const start = new Date(startDate).getTime();
    const end = new Date(endDate).getTime();
    const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 0;
  }

  /** 일일 임대료 */
  function getDailyPrice(): number {
    const daily = product.prices.find((p) => p.rentalUnit === "DAILY");
    return daily?.priceAmount ?? product.prices[0]?.priceAmount ?? 0;
  }

  const days = calcDays();
  const dailyPrice = getDailyPrice();
  const rentalFee = dailyPrice * days;
  const deposit = product.depositAmount ?? 0;
  const totalAmount = rentalFee + deposit;

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!startDate || !endDate) {
      setError("대여 기간을 입력해 주세요.");
      return;
    }
    if (!recipientName) {
      setError("수령인을 입력해 주세요.");
      return;
    }
    if (!recipientPhone) {
      setError("연락처를 입력해 주세요.");
      return;
    }
    if (!addressLine1) {
      setError("주소를 입력해 주세요.");
      return;
    }

    const request: CreateRentalRequest = {
      productId: product.id,
      startDate,
      endDate,
      deliveryInfo: {
        recipientName,
        recipientPhone,
        addressLine1,
        addressLine2: addressLine2 || undefined,
        zipCode,
      },
    };

    setIsSubmitting(true);
    try {
      const response = await client.post<{ rentalId: number }>(
        ENDPOINTS.RENTALS.BASE,
        request
      );
      router.push(`/rentals/${response.data.rentalId}`);
    } catch {
      setError("대여 신청에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {/* 대여 기간 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">대여 기간</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <div className="flex flex-col gap-1">
            <label htmlFor="startDate" className="text-sm font-medium">
              시작일
            </label>
            <Input
              id="startDate"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              min={new Date(Date.now() + 86400000).toISOString().split("T")[0]}
              required
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="endDate" className="text-sm font-medium">
              종료일
            </label>
            <Input
              id="endDate"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              min={startDate || new Date(Date.now() + 86400000).toISOString().split("T")[0]}
              required
            />
          </div>
          {days > 0 && (
            <p className="text-sm text-muted-foreground">기간: {days}일</p>
          )}
        </CardContent>
      </Card>

      {/* 배송지 정보 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">배송지 정보</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <div className="flex flex-col gap-1">
            <label htmlFor="recipientName" className="text-sm font-medium">
              수령인
            </label>
            <Input
              id="recipientName"
              type="text"
              placeholder="홍길동"
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="recipientPhone" className="text-sm font-medium">
              연락처
            </label>
            <Input
              id="recipientPhone"
              type="tel"
              placeholder="010-0000-0000"
              value={recipientPhone}
              onChange={(e) => setRecipientPhone(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="addressLine1" className="text-sm font-medium">
              주소
            </label>
            <Input
              id="addressLine1"
              type="text"
              placeholder="도로명 주소"
              value={addressLine1}
              onChange={(e) => setAddressLine1(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="addressLine2" className="text-sm font-medium">
              상세주소
            </label>
            <Input
              id="addressLine2"
              type="text"
              placeholder="상세 주소 (선택)"
              value={addressLine2}
              onChange={(e) => setAddressLine2(e.target.value)}
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="zipCode" className="text-sm font-medium">
              우편번호
            </label>
            <Input
              id="zipCode"
              type="text"
              placeholder="12345"
              value={zipCode}
              onChange={(e) => setZipCode(e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

      {/* 요금 요약 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">요금 요약</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">
              대여료: {dailyPrice.toLocaleString("ko-KR")}원/일 × {days}일
            </span>
            <span>{rentalFee.toLocaleString("ko-KR")}원</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">보증금</span>
            <span>{deposit.toLocaleString("ko-KR")}원</span>
          </div>
          <div className="border-t pt-2 flex justify-between font-semibold">
            <span>결제 예정</span>
            <span>{totalAmount.toLocaleString("ko-KR")}원</span>
          </div>
        </CardContent>
      </Card>

      {error && (
        <p className="text-sm text-destructive text-center">{error}</p>
      )}

      <Button type="submit" disabled={isSubmitting} className="w-full">
        {isSubmitting ? "신청 중..." : "신청하기"}
      </Button>
    </form>
  );
}
