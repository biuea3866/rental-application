"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

// ========================================
// 유효성 스키마
// ========================================

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, "이메일을 입력해주세요.")
    .email("올바른 이메일 형식을 입력해주세요."),
  password: z
    .string()
    .min(1, "비밀번호를 입력해주세요.")
    .min(8, "비밀번호는 8자 이상이어야 합니다."),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

// ========================================
// LoginForm 컴포넌트
// ========================================

interface LoginFormProps {
  onSubmit: (values: LoginFormValues) => Promise<void>;
  isLoading?: boolean;
  serverError?: string | null;
}

export function LoginForm({
  onSubmit,
  isLoading = false,
  serverError,
}: LoginFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-label="로그인 폼"
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

      <div className="space-y-1">
        <label htmlFor="login-email" className="text-sm font-medium">
          이메일
        </label>
        <Input
          id="login-email"
          type="email"
          placeholder="email@example.com"
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? "login-email-error" : undefined}
          {...register("email")}
        />
        {errors.email && (
          <p
            id="login-email-error"
            role="alert"
            className="text-xs text-destructive"
          >
            {errors.email.message}
          </p>
        )}
      </div>

      <div className="space-y-1">
        <label htmlFor="login-password" className="text-sm font-medium">
          비밀번호
        </label>
        <Input
          id="login-password"
          type="password"
          placeholder="비밀번호를 입력하세요"
          aria-invalid={!!errors.password}
          aria-describedby={
            errors.password ? "login-password-error" : undefined
          }
          {...register("password")}
        />
        {errors.password && (
          <p
            id="login-password-error"
            role="alert"
            className="text-xs text-destructive"
          >
            {errors.password.message}
          </p>
        )}
      </div>

      <Button
        type="submit"
        className="w-full"
        disabled={isLoading}
        aria-busy={isLoading}
      >
        {isLoading ? "로그인 중..." : "로그인"}
      </Button>
    </form>
  );
}
