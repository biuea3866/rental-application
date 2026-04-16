"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { createRentalApi } from "@/lib/api/rental";
import type { Product } from "@/lib/api/types";

// ========================================
// 폼 유효성 스키마
// ========================================

const rentalRequestSchema = z
  .object({
    startDate: z.string().min(1, "시작일을 선택해 주세요."),
    endDate: z.string().min(1, "종료일을 선택해 주세요."),
    recipientName: z
      .string()
      .min(2, "수령인 이름은 2자 이상이어야 합니다.")
      .max(20, "수령인 이름은 20자 이하이어야 합니다."),
    recipientPhone: z
      .string()
      .regex(/^010-\d{4}-\d{4}$/, "010-XXXX-XXXX 형식으로 입력해 주세요."),
    address: z.string().min(1, "주소를 입력해 주세요."),
    addressDetail: z.string(),
    zipCode: z.string().min(1, "우편번호를 입력해 주세요."),
  })
  .refine(
    (data) => {
      if (!data.startDate || !data.endDate) return true;
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const tomorrow = new Date(today);
      tomorrow.setDate(tomorrow.getDate() + 1);
      const start = new Date(data.startDate);
      return start >= tomorrow;
    },
    { message: "시작일은 오늘 이후여야 합니다.", path: ["startDate"] }
  )
  .refine(
    (data) => {
      if (!data.startDate || !data.endDate) return true;
      return new Date(data.endDate) > new Date(data.startDate);
    },
    { message: "종료일은 시작일 이후여야 합니다.", path: ["endDate"] }
  );

type RentalRequestFormValues = z.infer<typeof rentalRequestSchema>;

// ========================================
// RentalRequestForm 컴포넌트
// ========================================

interface RentalRequestFormProps {
  product: Product;
}

export function RentalRequestForm({ product }: RentalRequestFormProps) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const dailyPrice =
    product.prices.find((p) => p.rentalUnit === "DAILY")?.priceAmount ??
    product.prices[0]?.priceAmount ??
    0;

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RentalRequestFormValues>({
    resolver: zodResolver(rentalRequestSchema),
    defaultValues: {
      startDate: "",
      endDate: "",
      recipientName: "",
      recipientPhone: "",
      address: "",
      addressDetail: "",
      zipCode: "",
    },
  });

  const startDate = watch("startDate");
  const endDate = watch("endDate");

  const rentalDays =
    startDate && endDate
      ? Math.max(
          1,
          Math.ceil(
            (new Date(endDate).getTime() - new Date(startDate).getTime()) /
              (1000 * 60 * 60 * 24)
          )
        )
      : 0;

  const rentalFee = dailyPrice * rentalDays;
  const depositAmount = product.depositAmount ?? 0;
  const totalAmount = rentalFee + depositAmount;

  const onSubmit = async (data: RentalRequestFormValues) => {
    setIsSubmitting(true);
    try {
      const response = await createRentalApi({
        productId: product.id,
        startDate: data.startDate,
        endDate: data.endDate,
        deliveryInfo: {
          recipientName: data.recipientName,
          recipientPhone: data.recipientPhone,
          address: data.address,
          addressDetail: data.addressDetail ?? "",
          zipCode: data.zipCode,
        },
      });

      toast.success("대여 신청이 완료되었습니다. 등록자 승인을 기다려 주세요.");
      router.push(`/rentals/${response.data.rentalId}`);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "대여 신청에 실패했습니다.";
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      data-testid="rental-request-form"
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-6"
    >
      {/* 대여 기간 */}
      <section className="space-y-3">
        <h2 className="font-semibold text-base">대여 기간</h2>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <label htmlFor="startDate" className="text-sm text-muted-foreground">
              시작일
            </label>
            <Input
              id="startDate"
              type="date"
              data-testid="input-start-date"
              {...register("startDate")}
            />
            {errors.startDate && (
              <p className="text-xs text-destructive" role="alert">
                {errors.startDate.message}
              </p>
            )}
          </div>
          <div className="space-y-1">
            <label htmlFor="endDate" className="text-sm text-muted-foreground">
              종료일
            </label>
            <Input
              id="endDate"
              type="date"
              data-testid="input-end-date"
              {...register("endDate")}
            />
            {errors.endDate && (
              <p className="text-xs text-destructive" role="alert">
                {errors.endDate.message}
              </p>
            )}
          </div>
        </div>
        {rentalDays > 0 && (
          <p
            data-testid="rental-days"
            className="text-sm text-muted-foreground"
          >
            기간: {rentalDays}일
          </p>
        )}
      </section>

      {/* 배송지 정보 */}
      <section className="space-y-3">
        <h2 className="font-semibold text-base">배송지 정보</h2>
        <div className="space-y-1">
          <label
            htmlFor="recipientName"
            className="text-sm text-muted-foreground"
          >
            수령인
          </label>
          <Input
            id="recipientName"
            placeholder="수령인 이름"
            data-testid="input-recipient-name"
            {...register("recipientName")}
          />
          {errors.recipientName && (
            <p className="text-xs text-destructive" role="alert">
              {errors.recipientName.message}
            </p>
          )}
        </div>
        <div className="space-y-1">
          <label
            htmlFor="recipientPhone"
            className="text-sm text-muted-foreground"
          >
            연락처
          </label>
          <Input
            id="recipientPhone"
            placeholder="010-0000-0000"
            data-testid="input-recipient-phone"
            {...register("recipientPhone")}
          />
          {errors.recipientPhone && (
            <p className="text-xs text-destructive" role="alert">
              {errors.recipientPhone.message}
            </p>
          )}
        </div>
        <div className="grid grid-cols-3 gap-2">
          <div className="col-span-1 space-y-1">
            <label htmlFor="zipCode" className="text-sm text-muted-foreground">
              우편번호
            </label>
            <Input
              id="zipCode"
              placeholder="12345"
              data-testid="input-zip-code"
              {...register("zipCode")}
            />
            {errors.zipCode && (
              <p className="text-xs text-destructive" role="alert">
                {errors.zipCode.message}
              </p>
            )}
          </div>
          <div className="col-span-2 space-y-1">
            <label htmlFor="address" className="text-sm text-muted-foreground">
              주소
            </label>
            <Input
              id="address"
              placeholder="기본 주소"
              data-testid="input-address"
              {...register("address")}
            />
            {errors.address && (
              <p className="text-xs text-destructive" role="alert">
                {errors.address.message}
              </p>
            )}
          </div>
        </div>
        <div className="space-y-1">
          <label
            htmlFor="addressDetail"
            className="text-sm text-muted-foreground"
          >
            상세주소
          </label>
          <Input
            id="addressDetail"
            placeholder="상세 주소 (선택)"
            data-testid="input-address-detail"
            {...register("addressDetail")}
          />
        </div>
      </section>

      {/* 요금 요약 */}
      <section
        data-testid="fee-summary"
        className="rounded-lg border p-4 space-y-2 bg-muted/30"
      >
        <h2 className="font-semibold text-base mb-3">요금 요약</h2>
        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">
            대여료 ({dailyPrice.toLocaleString()}원/일 × {rentalDays || "-"}일)
          </span>
          <span>{rentalDays > 0 ? rentalFee.toLocaleString() : "-"}원</span>
        </div>
        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">보증금</span>
          <span>{depositAmount.toLocaleString()}원</span>
        </div>
        <hr className="my-1" />
        <div className="flex justify-between font-semibold">
          <span>결제 예정</span>
          <span
            data-testid="total-amount"
            className="text-primary"
          >
            {rentalDays > 0 ? totalAmount.toLocaleString() : "-"}원
          </span>
        </div>
      </section>

      {/* 신청하기 버튼 */}
      <Button
        type="submit"
        className="w-full"
        size="lg"
        disabled={isSubmitting}
        data-testid="submit-button"
      >
        {isSubmitting ? "신청 중..." : "신청하기"}
      </Button>
    </form>
  );
}
