"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useProductFormStore } from "@/stores/product-form-store";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/select";

// ========================================
// Zod 스키마
// ========================================

const step2Schema = z.object({
  deposit: z
    .string()
    .min(1, "보증금을 입력해 주세요.")
    .refine((v) => !isNaN(Number(v)) && Number(v) >= 0, "올바른 금액을 입력해 주세요."),
  pricePerDay: z
    .string()
    .min(1, "일 대여가를 입력해 주세요.")
    .refine((v) => !isNaN(Number(v)) && Number(v) > 0, "0보다 큰 금액을 입력해 주세요."),
  pricePerWeek: z
    .string()
    .optional()
    .refine(
      (v) => !v || (!isNaN(Number(v)) && Number(v) > 0),
      "올바른 금액을 입력해 주세요."
    ),
  pricePerMonth: z
    .string()
    .optional()
    .refine(
      (v) => !v || (!isNaN(Number(v)) && Number(v) > 0),
      "올바른 금액을 입력해 주세요."
    ),
});

type Step2FormValues = z.infer<typeof step2Schema>;

// ========================================
// Step2Pricing 컴포넌트
// ========================================

interface Step2PricingProps {
  onNext: () => void;
  onPrev: () => void;
}

function formatKRW(value: string): string {
  const num = parseInt(value.replace(/,/g, ""), 10);
  if (isNaN(num)) return value;
  return num.toLocaleString("ko-KR");
}

export function Step2Pricing({ onNext, onPrev }: Step2PricingProps) {
  const { step2, updateStep2, isSubmitting } = useProductFormStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = useForm<Step2FormValues>({
    resolver: zodResolver(step2Schema),
    defaultValues: {
      deposit: step2.deposit !== "" ? String(step2.deposit) : "",
      pricePerDay: step2.pricePerDay !== "" ? String(step2.pricePerDay) : "",
      pricePerWeek: step2.pricePerWeek !== "" ? String(step2.pricePerWeek) : "",
      pricePerMonth:
        step2.pricePerMonth !== "" ? String(step2.pricePerMonth) : "",
    },
  });

  const watchedValues = watch();
  useEffect(() => {
    updateStep2({
      deposit: watchedValues.deposit !== "" ? Number(watchedValues.deposit) : "",
      pricePerDay:
        watchedValues.pricePerDay !== "" ? Number(watchedValues.pricePerDay) : "",
      pricePerWeek:
        watchedValues.pricePerWeek ? Number(watchedValues.pricePerWeek) : "",
      pricePerMonth:
        watchedValues.pricePerMonth ? Number(watchedValues.pricePerMonth) : "",
    });
  }, [
    watchedValues.deposit,
    watchedValues.pricePerDay,
    watchedValues.pricePerWeek,
    watchedValues.pricePerMonth,
    updateStep2,
  ]);

  const onSubmit = (_data: Step2FormValues) => {
    onNext();
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      {/* 보증금 */}
      <div className="space-y-1.5">
        <Label htmlFor="deposit">
          보증금 <span className="text-destructive">*</span>
        </Label>
        <div className="relative">
          <Input
            id="deposit"
            type="number"
            min={0}
            placeholder="0"
            className="pr-8"
            aria-invalid={!!errors.deposit}
            {...register("deposit")}
          />
          <span className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
            원
          </span>
        </div>
        {errors.deposit && (
          <p className="text-xs text-destructive">{errors.deposit.message}</p>
        )}
        <p className="text-xs text-muted-foreground">
          대여 종료 후 상품 반납 시 환불됩니다.
        </p>
      </div>

      {/* 일 대여가 */}
      <div className="space-y-1.5">
        <Label htmlFor="pricePerDay">
          일 대여가 <span className="text-destructive">*</span>
        </Label>
        <div className="relative">
          <Input
            id="pricePerDay"
            type="number"
            min={1}
            placeholder="0"
            className="pr-8"
            aria-invalid={!!errors.pricePerDay}
            {...register("pricePerDay")}
          />
          <span className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
            원
          </span>
        </div>
        {errors.pricePerDay && (
          <p className="text-xs text-destructive">
            {errors.pricePerDay.message}
          </p>
        )}
      </div>

      {/* 주 대여가 (선택) */}
      <div className="space-y-1.5">
        <Label htmlFor="pricePerWeek">주 대여가 (선택)</Label>
        <div className="relative">
          <Input
            id="pricePerWeek"
            type="number"
            min={1}
            placeholder="미입력 시 일 대여가 × 7 적용"
            className="pr-8"
            aria-invalid={!!errors.pricePerWeek}
            {...register("pricePerWeek")}
          />
          <span className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
            원
          </span>
        </div>
        {errors.pricePerWeek && (
          <p className="text-xs text-destructive">
            {errors.pricePerWeek.message}
          </p>
        )}
      </div>

      {/* 월 대여가 (선택) */}
      <div className="space-y-1.5">
        <Label htmlFor="pricePerMonth">월 대여가 (선택)</Label>
        <div className="relative">
          <Input
            id="pricePerMonth"
            type="number"
            min={1}
            placeholder="미입력 시 일 대여가 × 30 적용"
            className="pr-8"
            aria-invalid={!!errors.pricePerMonth}
            {...register("pricePerMonth")}
          />
          <span className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
            원
          </span>
        </div>
        {errors.pricePerMonth && (
          <p className="text-xs text-destructive">
            {errors.pricePerMonth.message}
          </p>
        )}
      </div>

      <div className="flex justify-between pt-2">
        <Button type="button" variant="outline" onClick={onPrev} size="lg">
          이전
        </Button>
        <Button type="submit" disabled={isSubmitting} size="lg">
          {isSubmitting ? "저장 중..." : "다음 단계"}
        </Button>
      </div>
    </form>
  );
}
