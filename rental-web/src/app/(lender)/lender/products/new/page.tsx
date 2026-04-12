"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { useProductFormStore } from "@/stores/product-form-store";
import { useProductForm } from "@/hooks/use-product-form";
import { ProductFormStepper } from "@/components/product/ProductFormStepper";
import { Step1BasicInfo } from "@/components/product/Step1BasicInfo";
import { Step2Pricing } from "@/components/product/Step2Pricing";
import { Step3Images } from "@/components/product/Step3Images";
import { Step4Condition } from "@/components/product/Step4Condition";
import { Step5Review } from "@/components/product/Step5Review";

// ========================================
// 상품 등록 페이지 (5단계 스텝 폼)
// ========================================

export default function ProductNewPage() {
  const { currentStep, nextStep, prevStep } = useProductFormStore();
  const {
    saveStep1AndNext,
    saveStep2AndNext,
    saveStep3AndNext,
    saveStep4AndNext,
    submitDraft,
  } = useProductForm();

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      {/* 상단 네비게이션 */}
      <div className="mb-6 flex items-center gap-3">
        <Link
          href="/lender"
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          ← 대시보드
        </Link>
        <span className="text-muted-foreground">/</span>
        <span className="text-sm font-medium">상품 등록</span>
      </div>

      <h1 className="mb-6 text-xl font-bold">상품 등록</h1>

      {/* 단계 표시 */}
      <div className="mb-8">
        <ProductFormStepper currentStep={currentStep} />
      </div>

      {/* 단계별 폼 */}
      {currentStep === 1 && (
        <Step1BasicInfo onNext={saveStep1AndNext} />
      )}
      {currentStep === 2 && (
        <Step2Pricing onNext={saveStep2AndNext} onPrev={prevStep} />
      )}
      {currentStep === 3 && (
        <Step3Images onNext={saveStep3AndNext} onPrev={prevStep} />
      )}
      {currentStep === 4 && (
        <Step4Condition onNext={saveStep4AndNext} onPrev={prevStep} />
      )}
      {currentStep === 5 && (
        <Step5Review onSubmit={submitDraft} onPrev={prevStep} />
      )}
    </div>
  );
}
