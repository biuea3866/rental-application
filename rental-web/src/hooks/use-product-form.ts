"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { useProductFormStore } from "@/stores/product-form-store";
import {
  createDraftApi,
  updateDraftApi,
  submitDraftApi,
  getPresignedUrlApi,
  uploadToS3,
} from "@/lib/api/draft";
import type { ProductCategory, ProductCondition } from "@/lib/api/types";

// ========================================
// useProductForm 훅
// ========================================

export function useProductForm() {
  const store = useProductFormStore();
  const router = useRouter();

  /** 1단계 저장 후 DRAFT 생성 또는 업데이트 */
  const saveStep1AndNext = useCallback(async () => {
    const { step1, draftId } = store;

    store.setSubmitting(true);
    try {
      if (!draftId) {
        // 신규 DRAFT 생성
        const res = await createDraftApi({
          category: step1.category as ProductCategory,
          title: step1.title,
          description: step1.description,
          location: step1.location,
        });
        store.setDraftId(res.data.id);
      } else {
        await updateDraftApi(draftId, {
          category: step1.category as ProductCategory,
          title: step1.title,
          description: step1.description,
          location: step1.location,
        });
      }
      store.nextStep();
    } finally {
      store.setSubmitting(false);
    }
  }, [store]);

  /** 2단계 저장 */
  const saveStep2AndNext = useCallback(async () => {
    const { step2, draftId } = store;
    if (!draftId) return;

    store.setSubmitting(true);
    try {
      await updateDraftApi(draftId, {
        deposit: Number(step2.deposit),
        pricePerDay: Number(step2.pricePerDay),
        pricePerWeek: step2.pricePerWeek ? Number(step2.pricePerWeek) : undefined,
        pricePerMonth: step2.pricePerMonth
          ? Number(step2.pricePerMonth)
          : undefined,
      });
      store.nextStep();
    } finally {
      store.setSubmitting(false);
    }
  }, [store]);

  /** 3단계 저장 */
  const saveStep3AndNext = useCallback(async () => {
    const { step3, draftId } = store;
    if (!draftId) return;

    store.setSubmitting(true);
    try {
      await updateDraftApi(draftId, {
        imageUrls: step3.imageUrls,
      });
      store.nextStep();
    } finally {
      store.setSubmitting(false);
    }
  }, [store]);

  /** 4단계 저장 */
  const saveStep4AndNext = useCallback(async () => {
    const { step4, draftId } = store;
    if (!draftId) return;

    store.setSubmitting(true);
    try {
      await updateDraftApi(draftId, {
        condition: step4.condition as ProductCondition,
        conditionNote: step4.conditionNote,
      });
      store.nextStep();
    } finally {
      store.setSubmitting(false);
    }
  }, [store]);

  /** 최종 제출 */
  const submitDraft = useCallback(async () => {
    const { draftId } = store;
    if (!draftId) return;

    store.setSubmitting(true);
    try {
      await submitDraftApi(draftId);
      store.reset();
      router.push("/lender");
    } finally {
      store.setSubmitting(false);
    }
  }, [store, router]);

  /** 이미지 업로드 (Presigned URL 방식) */
  const uploadImage = useCallback(
    async (file: File): Promise<string> => {
      // 1. Presigned URL 획득
      const presignedRes = await getPresignedUrlApi({
        fileName: file.name,
        contentType: file.type,
        fileSize: file.size,
      });

      // 2. S3에 업로드
      await uploadToS3(presignedRes.data.presignedUrl, file);

      return presignedRes.data.fileUrl;
    },
    []
  );

  return {
    ...store,
    saveStep1AndNext,
    saveStep2AndNext,
    saveStep3AndNext,
    saveStep4AndNext,
    submitDraft,
    uploadImage,
  };
}
