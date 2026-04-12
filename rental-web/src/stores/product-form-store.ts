import { create } from "zustand";
import type { ProductCategory, ProductCondition } from "@/lib/api/types";

// ========================================
// 단계별 폼 데이터 타입
// ========================================

export interface Step1Data {
  category: ProductCategory | "";
  title: string;
  description: string;
  location: string;
}

export interface Step2Data {
  deposit: number | "";
  pricePerDay: number | "";
  pricePerWeek: number | "";
  pricePerMonth: number | "";
}

export interface Step3Data {
  imageUrls: string[];
  uploadingFiles: UploadingFile[];
}

export interface UploadingFile {
  id: string;
  file: File;
  progress: number;
  status: "pending" | "uploading" | "done" | "error";
  previewUrl: string;
  uploadedUrl?: string;
  error?: string;
}

export interface Step4Data {
  condition: ProductCondition | "";
  conditionNote: string;
}

export type ProductFormStep = 1 | 2 | 3 | 4 | 5;

// ========================================
// 폼 전체 상태 타입
// ========================================

export interface ProductFormState {
  currentStep: ProductFormStep;
  draftId: string | null;
  isSubmitting: boolean;
  step1: Step1Data;
  step2: Step2Data;
  step3: Step3Data;
  step4: Step4Data;
}

interface ProductFormActions {
  setStep: (step: ProductFormStep) => void;
  nextStep: () => void;
  prevStep: () => void;
  setDraftId: (id: string) => void;
  setSubmitting: (v: boolean) => void;

  updateStep1: (data: Partial<Step1Data>) => void;
  updateStep2: (data: Partial<Step2Data>) => void;
  updateStep3: (data: Partial<Step3Data>) => void;
  updateStep4: (data: Partial<Step4Data>) => void;

  addUploadingFile: (file: UploadingFile) => void;
  updateUploadingFile: (id: string, update: Partial<UploadingFile>) => void;
  removeUploadingFile: (id: string) => void;

  reset: () => void;
}

// ========================================
// 초기값
// ========================================

const initialState: ProductFormState = {
  currentStep: 1,
  draftId: null,
  isSubmitting: false,
  step1: {
    category: "",
    title: "",
    description: "",
    location: "",
  },
  step2: {
    deposit: "",
    pricePerDay: "",
    pricePerWeek: "",
    pricePerMonth: "",
  },
  step3: {
    imageUrls: [],
    uploadingFiles: [],
  },
  step4: {
    condition: "",
    conditionNote: "",
  },
};

// ========================================
// 스토어 생성
// ========================================

export const useProductFormStore = create<ProductFormState & ProductFormActions>(
  (set) => ({
    ...initialState,

    setStep: (step) => set({ currentStep: step }),

    nextStep: () =>
      set((state) => ({
        currentStep: Math.min(5, state.currentStep + 1) as ProductFormStep,
      })),

    prevStep: () =>
      set((state) => ({
        currentStep: Math.max(1, state.currentStep - 1) as ProductFormStep,
      })),

    setDraftId: (id) => set({ draftId: id }),

    setSubmitting: (v) => set({ isSubmitting: v }),

    updateStep1: (data) =>
      set((state) => ({ step1: { ...state.step1, ...data } })),

    updateStep2: (data) =>
      set((state) => ({ step2: { ...state.step2, ...data } })),

    updateStep3: (data) =>
      set((state) => ({ step3: { ...state.step3, ...data } })),

    updateStep4: (data) =>
      set((state) => ({ step4: { ...state.step4, ...data } })),

    addUploadingFile: (file) =>
      set((state) => ({
        step3: {
          ...state.step3,
          uploadingFiles: [...state.step3.uploadingFiles, file],
        },
      })),

    updateUploadingFile: (id, update) =>
      set((state) => ({
        step3: {
          ...state.step3,
          uploadingFiles: state.step3.uploadingFiles.map((f) =>
            f.id === id ? { ...f, ...update } : f
          ),
        },
      })),

    removeUploadingFile: (id) =>
      set((state) => ({
        step3: {
          ...state.step3,
          uploadingFiles: state.step3.uploadingFiles.filter((f) => f.id !== id),
        },
      })),

    reset: () => set(initialState),
  })
);
