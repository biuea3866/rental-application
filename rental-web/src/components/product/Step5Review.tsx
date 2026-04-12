"use client";

import { useProductFormStore } from "@/stores/product-form-store";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

// ========================================
// 카테고리 / 상태 한국어 매핑
// ========================================

const CATEGORY_LABELS: Record<string, string> = {
  ELECTRONICS: "전자기기",
  FURNITURE: "가구/인테리어",
  SPORTS: "스포츠/레저",
  FASHION: "패션/의류",
  BOOKS: "도서/교재",
  TOOLS: "공구/DIY",
  VEHICLES: "이동수단",
  OTHERS: "기타",
};

const CONDITION_LABELS: Record<string, string> = {
  NEW: "새 상품",
  LIKE_NEW: "거의 새 것",
  GOOD: "양호",
  FAIR: "보통",
  POOR: "낡음",
};

// ========================================
// Step5Review 컴포넌트
// ========================================

interface Step5ReviewProps {
  onSubmit: () => void;
  onPrev: () => void;
}

function ReviewRow({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-start gap-4 py-2.5 border-b last:border-0">
      <span className="w-24 shrink-0 text-sm text-muted-foreground">{label}</span>
      <span className="flex-1 text-sm">{value || <span className="text-muted-foreground">—</span>}</span>
    </div>
  );
}

export function Step5Review({ onSubmit, onPrev }: Step5ReviewProps) {
  const { step1, step2, step3, step4, isSubmitting } = useProductFormStore();

  return (
    <div className="space-y-6">
      <p className="text-sm text-muted-foreground">
        등록 내용을 최종 확인하고 검수 제출을 해 주세요. 검수 승인 후 상품이 공개됩니다.
      </p>

      {/* 기본 정보 */}
      <Card>
        <CardHeader className="border-b pb-3">
          <CardTitle className="text-sm">기본 정보</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          <ReviewRow
            label="카테고리"
            value={CATEGORY_LABELS[step1.category] || step1.category}
          />
          <ReviewRow label="상품명" value={step1.title} />
          <ReviewRow label="상품 설명" value={step1.description} />
          <ReviewRow label="위치" value={step1.location} />
        </CardContent>
      </Card>

      {/* 가격 */}
      <Card>
        <CardHeader className="border-b pb-3">
          <CardTitle className="text-sm">가격 정보</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          <ReviewRow
            label="보증금"
            value={
              step2.deposit !== ""
                ? `${Number(step2.deposit).toLocaleString("ko-KR")}원`
                : undefined
            }
          />
          <ReviewRow
            label="일 대여가"
            value={
              step2.pricePerDay !== ""
                ? `${Number(step2.pricePerDay).toLocaleString("ko-KR")}원`
                : undefined
            }
          />
          <ReviewRow
            label="주 대여가"
            value={
              step2.pricePerWeek
                ? `${Number(step2.pricePerWeek).toLocaleString("ko-KR")}원`
                : "미설정 (일 × 7)"
            }
          />
          <ReviewRow
            label="월 대여가"
            value={
              step2.pricePerMonth
                ? `${Number(step2.pricePerMonth).toLocaleString("ko-KR")}원`
                : "미설정 (일 × 30)"
            }
          />
        </CardContent>
      </Card>

      {/* 이미지 */}
      <Card>
        <CardHeader className="border-b pb-3">
          <CardTitle className="text-sm">이미지</CardTitle>
        </CardHeader>
        <CardContent className="pt-3">
          {step3.imageUrls.length === 0 ? (
            <p className="text-sm text-muted-foreground">등록된 이미지 없음</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {step3.imageUrls.map((url, idx) => (
                <div
                  key={url}
                  className="relative h-20 w-20 overflow-hidden rounded-md border"
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={url}
                    alt={`이미지 ${idx + 1}`}
                    className="h-full w-full object-cover"
                  />
                  {idx === 0 && (
                    <span className="absolute left-0.5 top-0.5 rounded bg-primary px-1 py-0.5 text-[9px] text-primary-foreground">
                      대표
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* 상품 상태 */}
      <Card>
        <CardHeader className="border-b pb-3">
          <CardTitle className="text-sm">상품 상태</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          <ReviewRow
            label="상태"
            value={CONDITION_LABELS[step4.condition] || step4.condition}
          />
          <ReviewRow label="메모" value={step4.conditionNote} />
        </CardContent>
      </Card>

      <div className="flex justify-between pt-2">
        <Button type="button" variant="outline" onClick={onPrev} size="lg">
          이전
        </Button>
        <Button
          type="button"
          onClick={onSubmit}
          disabled={isSubmitting}
          size="lg"
          className="bg-primary"
        >
          {isSubmitting ? "제출 중..." : "검수 제출"}
        </Button>
      </div>
    </div>
  );
}
