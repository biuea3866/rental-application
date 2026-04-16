"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { PaymentWidget } from "@/components/rental/PaymentWidget";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { RentalDetail, PaymentMethod } from "@/lib/api/types";

// ========================================
// 결제 페이지 — /rentals/[id]/payment
// 진입 조건: RentalStatus = APPROVED
// ========================================

export default function RentalPaymentPage() {
  const params = useParams();
  const router = useRouter();
  const rentalId = params.id as string;
  const client = getApiClient();

  const [isProcessing, setIsProcessing] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["rental", rentalId],
    queryFn: async () => {
      const response = await client.get<RentalDetail>(
        ENDPOINTS.RENTALS.BY_ID(rentalId)
      );
      return response.data;
    },
    enabled: Boolean(rentalId),
  });

  if (isLoading) {
    return (
      <main className="flex flex-1 flex-col p-6">
        <p className="text-sm text-muted-foreground">불러오는 중...</p>
      </main>
    );
  }

  if (isError || !data) {
    return (
      <main className="flex flex-1 flex-col p-6">
        <p className="text-sm text-destructive">
          대여 정보를 불러오지 못했습니다. 다시 시도해주세요.
        </p>
      </main>
    );
  }

  if (data.status !== "APPROVED") {
    return (
      <main className="flex flex-1 flex-col p-6">
        <p className="text-sm text-destructive">
          결제 가능한 상태가 아닙니다. (현재 상태: {data.status})
        </p>
      </main>
    );
  }

  /** orderId: RC-{rentalId}-{timestamp} 형식 */
  const orderId = `RC-${data.rentalId}-${Date.now()}`;
  /** 결제 총액 = 대여료 + 보증금 */
  const totalAmount = data.totalAmount + data.depositAmount;
  /** 대여료 = totalAmount에서 보증금 제외 */
  const rentalFee = data.totalAmount;

  async function handlePaymentSuccess(
    paymentKey: string,
    paidOrderId: string,
    amount: number,
    method: PaymentMethod
  ) {
    setPaymentError(null);
    setIsProcessing(true);
    try {
      await client.post(ENDPOINTS.RENTALS.PAYMENT(rentalId), {
        paymentKey,
        orderId: paidOrderId,
        amount,
        paymentMethod: method,
      });
      router.push(`/rentals/${rentalId}/payment/success`);
    } catch {
      setPaymentError("결제 처리에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setIsProcessing(false);
    }
  }

  function handlePaymentFail(error: string) {
    setPaymentError(error);
  }

  return (
    <main className="flex flex-1 flex-col gap-4 p-6">
      <h1 className="text-2xl font-bold">결제</h1>

      {/* 주문 정보 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">주문 정보</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">상품명</span>
            <span className="font-medium">{data.product.name}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">대여 기간</span>
            <span>
              {data.startDate} ~ {data.endDate}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">배송지</span>
            <span className="max-w-[60%] text-right">
              {data.deliveryInfo.addressLine1}
              {data.deliveryInfo.addressLine2
                ? ` ${data.deliveryInfo.addressLine2}`
                : ""}
            </span>
          </div>
        </CardContent>
      </Card>

      {/* Toss Payments 결제 위젯 */}
      <PaymentWidget
        rentalId={data.rentalId}
        totalAmount={totalAmount}
        orderId={orderId}
        onSuccess={handlePaymentSuccess}
        onFail={handlePaymentFail}
        isProcessing={isProcessing}
      />

      {/* 최종 결제 금액 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">최종 결제 금액</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">대여료</span>
            <span>{rentalFee.toLocaleString("ko-KR")}원</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">보증금</span>
            <span>{data.depositAmount.toLocaleString("ko-KR")}원</span>
          </div>
          <div className="border-t pt-2 flex justify-between font-semibold">
            <span>합계</span>
            <span>{totalAmount.toLocaleString("ko-KR")}원</span>
          </div>
        </CardContent>
      </Card>

      {paymentError && (
        <p className="text-sm text-destructive text-center">{paymentError}</p>
      )}
    </main>
  );
}
