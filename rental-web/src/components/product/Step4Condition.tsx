"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useProductFormStore } from "@/stores/product-form-store";
import { Button } from "@/components/ui/button";
import { Label, Textarea } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import type { ProductCondition } from "@/lib/api/types";

// ========================================
// 상품 상태 목록
// ========================================

const CONDITIONS: { value: ProductCondition; label: string; desc: string }[] = [
  { value: "NEW", label: "새 상품", desc: "미개봉 또는 사용 흔적 전혀 없음" },
  { value: "LIKE_NEW", label: "거의 새 것", desc: "사용했지만 새 것과 같은 상태" },
  { value: "GOOD", label: "양호", desc: "약간의 사용감, 기능 이상 없음" },
  { value: "FAIR", label: "보통", desc: "눈에 띄는 사용감, 기능 정상" },
  { value: "POOR", label: "낡음", desc: "심한 사용감, 기능은 정상" },
];

// ========================================
// Zod 스키마
// ========================================

const step4Schema = z.object({
  condition: z
    .string()
    .refine(
      (v) => ["NEW", "LIKE_NEW", "GOOD", "FAIR", "POOR"].includes(v),
      { message: "상품 상태를 선택해 주세요." }
    ),
  conditionNote: z
    .string()
    .max(500, "500자 이하로 입력해 주세요."),
});

type Step4FormValues = z.infer<typeof step4Schema>;

// ========================================
// Step4Condition 컴포넌트
// ========================================

interface Step4ConditionProps {
  onNext: () => void;
  onPrev: () => void;
}

export function Step4Condition({ onNext, onPrev }: Step4ConditionProps) {
  const { step4, updateStep4, isSubmitting } = useProductFormStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = useForm<Step4FormValues>({
    resolver: zodResolver(step4Schema),
    defaultValues: {
      condition: step4.condition || "",
      conditionNote: step4.conditionNote || "",
    },
  });

  const selectedCondition = watch("condition");

  useEffect(() => {
    const sub = watch((values) => {
      updateStep4({
        condition: (values.condition || "") as ProductCondition | "",
        conditionNote: values.conditionNote || "",
      });
    });
    return () => sub.unsubscribe();
  }, [watch, updateStep4]);

  const onSubmit = (_data: Step4FormValues) => {
    onNext();
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      {/* 상품 상태 선택 */}
      <div className="space-y-2">
        <Label>
          상품 상태 <span className="text-destructive">*</span>
        </Label>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {CONDITIONS.map(({ value, label, desc }) => (
            <label
              key={value}
              data-testid={`condition-${value}`}
              className={cn(
                "flex cursor-pointer flex-col rounded-lg border p-3 transition-colors",
                selectedCondition === value
                  ? "border-primary bg-primary/5"
                  : "border-border hover:border-primary/50"
              )}
            >
              <input
                type="radio"
                value={value}
                className="sr-only"
                {...register("condition")}
              />
              <span className="text-sm font-medium">{label}</span>
              <span className="mt-0.5 text-xs text-muted-foreground">{desc}</span>
            </label>
          ))}
        </div>
        {errors.condition && (
          <p className="text-xs text-destructive">{errors.condition.message}</p>
        )}
      </div>

      {/* 사용감 메모 */}
      <div className="space-y-1.5">
        <Label htmlFor="conditionNote">사용감 메모 (선택)</Label>
        <Textarea
          id="conditionNote"
          rows={3}
          placeholder="예) 구매 후 5회 사용. 배터리 90% 이상. 충전기 포함."
          {...register("conditionNote")}
        />
        {errors.conditionNote && (
          <p className="text-xs text-destructive">
            {errors.conditionNote.message}
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
