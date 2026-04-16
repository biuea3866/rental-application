"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { processPaymentApi } from "@/lib/api/rental";
import type { Rental } from "@/lib/api/types";

// ========================================
// PaymentWidget 컴포넌트
// Toss Payments SDK 연동 (placeholder + 실제 흐름)
// ========================================

interface PaymentWidgetProps {
  rental: Rental;
  onSuccess?: (rentalId: number) => void;
}

export function PaymentWidget({ rental, onSuccess }: PaymentWidgetProps) {
  const router = useRouter();
  const [isProcessing, setIsProcessing] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<"card" | "transfer" | "tosspay">(
    "card"
  );

  const handlePayment = async () => {
    setIsProcessing(true);
    try {
      // Toss Payments SDK 결제 요청 시뮬레이션
      // 실제 환경에서는:
      //   const tossPayments = await loadTossPayments(process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY!);
      //   const payment = await tossPayments.requestPayment({ ... });
      //   → paymentKey, orderId, amount를 BE로 전송

      const orderId = `RC-${rental.id}-${Date.now()}`;
      const paymentKey = `toss-${paymentMethod}-${orderId}`;

      const response = await processPaymentApi(rental.id, {
        paymentKey,
        orderId,
        amount: rental.totalAmount,
      });

      toast.success("결제가 완료되었습니다.");
      if (onSuccess) {
        onSuccess(rental.id);
      } else {
        router.push(`/rentals/${response.data.id}`);
      }
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "결제에 실패했습니다.";
      toast.error(message + " 다시 시도해 주세요.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div data-testid="payment-widget" className="space-y-4">
      {/* 결제 수단 선택 */}
      <div className="rounded-lg border p-4 space-y-3">
        <h3 className="font-semibold text-sm">결제 수단</h3>
        <div className="flex gap-2">
          {(
            [
              { value: "card", label: "카드" },
              { value: "transfer", label: "계좌이체" },
              { value: "tosspay", label: "토스페이" },
            ] as const
          ).map((method) => (
            <button
              key={method.value}
              type="button"
              data-testid={`payment-method-${method.value}`}
              onClick={() => setPaymentMethod(method.value)}
              className={`flex-1 rounded-lg border py-2.5 text-sm font-medium transition-colors
                ${
                  paymentMethod === method.value
                    ? "border-primary bg-primary/5 text-primary"
                    : "border-border text-muted-foreground hover:border-primary/50"
                }`}
            >
              {method.label}
            </button>
          ))}
        </div>

        {/* Toss Payments 위젯 플레이스홀더 */}
        <div
          data-testid="toss-widget-placeholder"
          className="rounded-md border border-dashed border-muted-foreground/30 p-6 text-center text-xs text-muted-foreground"
        >
          Toss Payments 결제 위젯
          <br />
          <span className="text-[10px]">
            (실제 환경에서는 SDK iframe이 여기 렌더링됩니다)
          </span>
        </div>
      </div>

      {/* 결제하기 버튼 */}
      <Button
        type="button"
        className="w-full"
        size="lg"
        disabled={isProcessing}
        onClick={handlePayment}
        data-testid="pay-button"
      >
        {isProcessing
          ? "결제 처리 중..."
          : `결제하기 ${rental.totalAmount.toLocaleString()}원`}
      </Button>
    </div>
  );
}
