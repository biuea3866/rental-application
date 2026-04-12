"use client";

import { cn } from "@/lib/utils";
import type { ProductFormStep } from "@/stores/product-form-store";

// ========================================
// 5단계 스텝 UI
// ========================================

const STEPS: { step: ProductFormStep; label: string }[] = [
  { step: 1, label: "기본 정보" },
  { step: 2, label: "가격 설정" },
  { step: 3, label: "이미지" },
  { step: 4, label: "상태 설명" },
  { step: 5, label: "최종 확인" },
];

interface ProductFormStepperProps {
  currentStep: ProductFormStep;
}

export function ProductFormStepper({ currentStep }: ProductFormStepperProps) {
  return (
    <nav aria-label="상품 등록 단계" className="w-full">
      <ol className="flex items-center">
        {STEPS.map(({ step, label }, idx) => {
          const isCompleted = currentStep > step;
          const isCurrent = currentStep === step;

          return (
            <li
              key={step}
              className={cn(
                "flex items-center",
                idx < STEPS.length - 1 && "flex-1"
              )}
            >
              {/* Step circle */}
              <div className="flex flex-col items-center">
                <div
                  data-testid={`step-indicator-${step}`}
                  aria-current={isCurrent ? "step" : undefined}
                  className={cn(
                    "flex h-8 w-8 items-center justify-center rounded-full border-2 text-sm font-semibold transition-colors",
                    isCompleted &&
                      "border-primary bg-primary text-primary-foreground",
                    isCurrent &&
                      "border-primary bg-background text-primary",
                    !isCompleted &&
                      !isCurrent &&
                      "border-muted-foreground/30 bg-background text-muted-foreground"
                  )}
                >
                  {isCompleted ? (
                    <svg
                      className="h-4 w-4"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2.5}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M5 13l4 4L19 7"
                      />
                    </svg>
                  ) : (
                    step
                  )}
                </div>
                <span
                  className={cn(
                    "mt-1 text-xs whitespace-nowrap",
                    isCurrent
                      ? "font-medium text-primary"
                      : "text-muted-foreground"
                  )}
                >
                  {label}
                </span>
              </div>

              {/* Connector line */}
              {idx < STEPS.length - 1 && (
                <div
                  className={cn(
                    "mb-5 flex-1 border-t-2 transition-colors",
                    isCompleted ? "border-primary" : "border-muted-foreground/20"
                  )}
                />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
