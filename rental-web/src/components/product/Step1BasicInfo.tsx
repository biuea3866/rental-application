"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useProductFormStore } from "@/stores/product-form-store";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, Label, Textarea } from "@/components/ui/select";
import type { ProductCategory } from "@/lib/api/types";

// ========================================
// 카테고리 목록
// ========================================

const CATEGORIES: { value: ProductCategory; label: string }[] = [
  { value: "ELECTRONICS", label: "전자기기" },
  { value: "FURNITURE", label: "가구/인테리어" },
  { value: "SPORTS", label: "스포츠/레저" },
  { value: "FASHION", label: "패션/의류" },
  { value: "BOOKS", label: "도서/교재" },
  { value: "TOOLS", label: "공구/DIY" },
  { value: "VEHICLES", label: "이동수단" },
  { value: "OTHERS", label: "기타" },
];

// ========================================
// Zod 스키마
// ========================================

const step1Schema = z.object({
  category: z
    .string()
    .refine(
      (v) =>
        ["ELECTRONICS", "FURNITURE", "SPORTS", "FASHION", "BOOKS", "TOOLS", "VEHICLES", "OTHERS"].includes(v),
      { message: "카테고리를 선택해 주세요." }
    ),
  title: z
    .string()
    .min(2, "상품명은 2자 이상 입력해 주세요.")
    .max(100, "상품명은 100자 이하로 입력해 주세요."),
  description: z
    .string()
    .min(10, "상품 설명은 10자 이상 입력해 주세요.")
    .max(2000, "상품 설명은 2000자 이하로 입력해 주세요."),
  location: z.string().min(2, "위치를 입력해 주세요."),
});

type Step1FormValues = z.infer<typeof step1Schema>;

// ========================================
// Step1BasicInfo 컴포넌트
// ========================================

interface Step1BasicInfoProps {
  onNext: () => void;
}

export function Step1BasicInfo({ onNext }: Step1BasicInfoProps) {
  const { step1, updateStep1, isSubmitting } = useProductFormStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = useForm<Step1FormValues>({
    resolver: zodResolver(step1Schema),
    defaultValues: {
      category: step1.category || "",
      title: step1.title,
      description: step1.description,
      location: step1.location,
    },
  });

  // 폼 값 변경 시 스토어 동기화
  const watchedValues = watch();
  useEffect(() => {
    updateStep1({
      category: (watchedValues.category || "") as ProductCategory | "",
      title: watchedValues.title || "",
      description: watchedValues.description || "",
      location: watchedValues.location || "",
    });
  }, [
    watchedValues.category,
    watchedValues.title,
    watchedValues.description,
    watchedValues.location,
    updateStep1,
  ]);

  const onSubmit = (_data: Step1FormValues) => {
    onNext();
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      {/* 카테고리 */}
      <div className="space-y-1.5">
        <Label htmlFor="category">
          카테고리 <span className="text-destructive">*</span>
        </Label>
        <Select
          id="category"
          aria-invalid={!!errors.category}
          {...register("category")}
        >
          <option value="">카테고리 선택</option>
          {CATEGORIES.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
        {errors.category && (
          <p className="text-xs text-destructive">{errors.category.message}</p>
        )}
      </div>

      {/* 상품명 */}
      <div className="space-y-1.5">
        <Label htmlFor="title">
          상품명 <span className="text-destructive">*</span>
        </Label>
        <Input
          id="title"
          placeholder="예) 소니 A7C II 미러리스 카메라"
          aria-invalid={!!errors.title}
          {...register("title")}
        />
        {errors.title && (
          <p className="text-xs text-destructive">{errors.title.message}</p>
        )}
      </div>

      {/* 상품 설명 */}
      <div className="space-y-1.5">
        <Label htmlFor="description">
          상품 설명 <span className="text-destructive">*</span>
        </Label>
        <Textarea
          id="description"
          rows={5}
          placeholder="상품에 대한 상세 설명을 입력해 주세요. (포함 액세서리, 사용 방법, 주의 사항 등)"
          aria-invalid={!!errors.description}
          {...register("description")}
        />
        {errors.description && (
          <p className="text-xs text-destructive">
            {errors.description.message}
          </p>
        )}
      </div>

      {/* 위치 */}
      <div className="space-y-1.5">
        <Label htmlFor="location">
          위치 <span className="text-destructive">*</span>
        </Label>
        <Input
          id="location"
          placeholder="예) 서울 강남구"
          aria-invalid={!!errors.location}
          {...register("location")}
        />
        {errors.location && (
          <p className="text-xs text-destructive">{errors.location.message}</p>
        )}
      </div>

      <div className="flex justify-end pt-2">
        <Button type="submit" disabled={isSubmitting} size="lg">
          {isSubmitting ? "저장 중..." : "다음 단계"}
        </Button>
      </div>
    </form>
  );
}
