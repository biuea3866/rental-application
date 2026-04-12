"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { UserRole } from "@/lib/api/types";

// ========================================
// 유효성 스키마
// ========================================

export const signupSchema = z
  .object({
    email: z
      .string()
      .min(1, "이메일을 입력해주세요.")
      .email("올바른 이메일 형식을 입력해주세요."),
    name: z
      .string()
      .min(1, "이름을 입력해주세요.")
      .min(2, "이름은 2자 이상이어야 합니다."),
    phone: z
      .string()
      .min(1, "전화번호를 입력해주세요.")
      .regex(/^010-\d{4}-\d{4}$/, "올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678)"),
    password: z
      .string()
      .min(1, "비밀번호를 입력해주세요.")
      .min(8, "비밀번호는 8자 이상이어야 합니다."),
    passwordConfirm: z.string().min(1, "비밀번호 확인을 입력해주세요."),
    role: z.enum(["LENDER", "RENTER"] as const),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: "비밀번호가 일치하지 않습니다.",
    path: ["passwordConfirm"],
  });

export type SignupFormValues = z.infer<typeof signupSchema>;

// ========================================
// SignupForm 컴포넌트
// ========================================

interface SignupFormProps {
  onSubmit: (values: SignupFormValues) => Promise<void>;
  isLoading?: boolean;
  serverError?: string | null;
}

export function SignupForm({
  onSubmit,
  isLoading = false,
  serverError,
}: SignupFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    defaultValues: {
      email: "",
      name: "",
      phone: "",
      password: "",
      passwordConfirm: "",
      role: "RENTER" as UserRole,
    },
  });

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-label="회원가입 폼"
      className="space-y-4"
    >
      {serverError && (
        <div
          role="alert"
          className="rounded-md bg-destructive/10 p-3 text-sm text-destructive"
        >
          {serverError}
        </div>
      )}

      {/* 이메일 */}
      <div className="space-y-1">
        <label htmlFor="signup-email" className="text-sm font-medium">
          이메일
        </label>
        <Input
          id="signup-email"
          type="email"
          placeholder="email@example.com"
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? "signup-email-error" : undefined}
          {...register("email")}
        />
        {errors.email && (
          <p
            id="signup-email-error"
            role="alert"
            className="text-xs text-destructive"
          >
            {errors.email.message}
          </p>
        )}
      </div>

      {/* 이름 */}
      <div className="space-y-1">
        <label htmlFor="signup-name" className="text-sm font-medium">
          이름
        </label>
        <Input
          id="signup-name"
          type="text"
          placeholder="홍길동"
          aria-invalid={!!errors.name}
          aria-describedby={errors.name ? "signup-name-error" : undefined}
          {...register("name")}
        />
        {errors.name && (
          <p
            id="signup-name-error"
            role="alert"
            className="text-xs text-destructive"
          >
            {errors.name.message}
          </p>
        )}
      </div>

      {/* 전화번호 */}
      <div className="space-y-1">
        <label htmlFor="signup-phone" className="text-sm font-medium">
          전화번호
        </label>
        <Input
          id="signup-phone"
          type="tel"
          placeholder="010-1234-5678"
          aria-invalid={!!errors.phone}
          aria-describedby={errors.phone ? "signup-phone-error" : undefined}
          {...register("phone")}
        />
        {errors.phone && (
          <p
            id="signup-phone-error"
            role="alert"
            className="text-xs text-destructive"
          >
            {errors.phone.message}
          </p>
        )}
      </div>

      {/* 비밀번호 */}
      <div className="space-y-1">
        <label htmlFor="signup-password" className="text-sm font-medium">
          비밀번호
        </label>
        <Input
          id="signup-password"
          type="password"
          placeholder="8자 이상"
          aria-invalid={!!errors.password}
          aria-describedby={
            errors.password ? "signup-password-error" : undefined
          }
          {...register("password")}
        />
        {errors.password && (
          <p
            id="signup-password-error"
            role="alert"
            className="text-xs text-destructive"
          >
            {errors.password.message}
          </p>
        )}
      </div>

      {/* 비밀번호 확인 */}
      <div className="space-y-1">
        <label
          htmlFor="signup-password-confirm"
          className="text-sm font-medium"
        >
          비밀번호 확인
        </label>
        <Input
          id="signup-password-confirm"
          type="password"
          placeholder="비밀번호를 다시 입력하세요"
          aria-invalid={!!errors.passwordConfirm}
          aria-describedby={
            errors.passwordConfirm ? "signup-password-confirm-error" : undefined
          }
          {...register("passwordConfirm")}
        />
        {errors.passwordConfirm && (
          <p
            id="signup-password-confirm-error"
            role="alert"
            className="text-xs text-destructive"
          >
            {errors.passwordConfirm.message}
          </p>
        )}
      </div>

      {/* 역할 선택 */}
      <div className="space-y-1">
        <label htmlFor="signup-role" className="text-sm font-medium">
          역할 선택
        </label>
        <select
          id="signup-role"
          aria-invalid={!!errors.role}
          className="flex h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          {...register("role")}
        >
          <option value="RENTER">대여자 (Renter)</option>
          <option value="LENDER">등록자 (Lender)</option>
        </select>
        {errors.role && (
          <p role="alert" className="text-xs text-destructive">
            {errors.role.message}
          </p>
        )}
      </div>

      <Button
        type="submit"
        className="w-full"
        disabled={isLoading}
        aria-busy={isLoading}
      >
        {isLoading ? "가입 중..." : "회원가입"}
      </Button>
    </form>
  );
}
