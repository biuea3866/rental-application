"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import type { PaymentMethod } from "@/lib/api/types";

// ========================================
// Toss Payments 결제 위젯 (Placeholder)
// 실제 연동 시 @tosspayments/tosspayments-sdk 사용
// ========================================

interface PaymentWidgetProps {
  rentalId: number;
  totalAmount: number;
  orderId: string;
  onSuccess: (paymentKey: string, orderId: string, amount: number, method: PaymentMethod) => void;
  onFail: (error: string) => void;
  isProcessing: boolean;
}

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CARD", label: "카드" },
  { value: "BANK_TRANSFER", label: "계좌이체" },
  { value: "TOSS_PAY", label: "토스페이" },
  { value: "KAKAO_PAY", label: "카카오페이" },
];

export function PaymentWidget({
  rentalId,
  totalAmount,
  orderId,
  onSuccess,
  onFail,
  isProcessing,
}: PaymentWidgetProps) {
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>("CARD");

  function handlePay() {
    if (isProcessing) return;

    // Toss Payments 실제 연동 전까지 stub paymentKey 사용
    const stubPaymentKey = `stub-payment-key-${rentalId}-${Date.now()}`;
    onSuccess(stubPaymentKey, orderId, totalAmount, selectedMethod);

    void onFail; // 실패 시나리오는 실제 PG 연동 후 처리
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Toss Payments 결제 위젯</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {/* 결제 수단 선택 */}
        <div className="flex flex-wrap gap-2">
          {PAYMENT_METHODS.map((method) => (
            <button
              key={method.value}
              type="button"
              onClick={() => setSelectedMethod(method.value)}
              className={`rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors ${
                selectedMethod === method.value
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-background hover:bg-muted"
              }`}
            >
              {method.label}
            </button>
          ))}
        </div>

        {/* 결제 안내 */}
        <div className="rounded-lg bg-muted p-3 text-xs text-muted-foreground">
          <p>선택 결제 수단: {PAYMENT_METHODS.find((m) => m.value === selectedMethod)?.label}</p>
          <p className="mt-1">Toss Payments 위젯은 실제 연동 후 활성화됩니다.</p>
        </div>

        <Button
          type="button"
          onClick={handlePay}
          disabled={isProcessing}
          className="w-full"
        >
          {isProcessing
            ? "결제 처리 중..."
            : `결제하기 ${totalAmount.toLocaleString("ko-KR")}원`}
        </Button>
      </CardContent>
    </Card>
  );
}
