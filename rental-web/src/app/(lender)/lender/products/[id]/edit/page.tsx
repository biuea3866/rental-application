"use client";

import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { getDraftApi } from "@/lib/api/draft";
import { useProductFormStore } from "@/stores/product-form-store";
import { useProductForm } from "@/hooks/use-product-form";
import { ProductFormStepper } from "@/components/product/ProductFormStepper";
import { Step1BasicInfo } from "@/components/product/Step1BasicInfo";
import { Step2Pricing } from "@/components/product/Step2Pricing";
import { Step3Images } from "@/components/product/Step3Images";
import { Step4Condition } from "@/components/product/Step4Condition";
import { Step5Review } from "@/components/product/Step5Review";
import type { ProductDraft } from "@/lib/api/types";

// ========================================
// 상품 수정 페이지
// ========================================

export default function ProductEditPage() {
  const params = useParams<{ id: string }>();
  const draftId = params.id;

  const {
    currentStep,
    prevStep,
    setDraftId,
    updateStep1,
    updateStep2,
    updateStep3,
    updateStep4,
  } = useProductFormStore();

  const {
    saveStep1AndNext,
    saveStep2AndNext,
    saveStep3AndNext,
    saveStep4AndNext,
    submitDraft,
  } = useProductForm();

  // DRAFT 데이터 조회
  const { data, isLoading, isError } = useQuery({
    queryKey: ["draft", draftId],
    queryFn: () => getDraftApi(draftId),
    enabled: !!draftId,
  });

  // 스토어에 기존 데이터 세팅
  useEffect(() => {
    if (!data?.data) return;
    const draft: ProductDraft = data.data;

    setDraftId(draft.id);
    updateStep1({
      category: draft.category,
      title: draft.title,
      description: draft.description,
      location: draft.location,
    });
    updateStep2({
      deposit: draft.deposit,
      pricePerDay: draft.pricePerDay,
      pricePerWeek: draft.pricePerWeek ?? "",
      pricePerMonth: draft.pricePerMonth ?? "",
    });
    updateStep3({ imageUrls: draft.imageUrls });
    updateStep4({
      condition: draft.condition,
      conditionNote: draft.conditionNote,
    });
  }, [data, setDraftId, updateStep1, updateStep2, updateStep3, updateStep4]);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <div className="space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-12 animate-pulse rounded-xl bg-muted" />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <p className="text-sm text-destructive">
          상품 정보를 불러오지 못했습니다.
        </p>
        <Link href="/lender" className="mt-2 text-sm underline">
          대시보드로 돌아가기
        </Link>
      </div>
    );
  }

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
        <span className="text-sm font-medium">상품 수정</span>
      </div>

      <h1 className="mb-6 text-xl font-bold">상품 수정</h1>

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
