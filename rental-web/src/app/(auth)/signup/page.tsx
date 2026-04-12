"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "@/components/ui/card";
import { SignupForm } from "@/components/auth/SignupForm";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { SignupFormValues } from "@/components/auth/SignupForm";

// ========================================
// 회원가입 페이지
// ========================================

export default function SignupPage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const handleSignup = async (values: SignupFormValues) => {
    setIsLoading(true);
    setServerError(null);

    const apiClient = getApiClient();

    try {
      // 회원가입 요청 → 인증코드 발송
      await apiClient.post(
        ENDPOINTS.AUTH.SIGNUP,
        {
          email: values.email,
          password: values.password,
          name: values.name,
          phone: values.phone,
          role: values.role,
        },
        { requiresAuth: false }
      );

      // 인증코드 페이지로 이동 (전화번호 전달)
      const params = new URLSearchParams({ phone: values.phone });
      router.push(`/signup/verify?${params.toString()}`);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "회원가입에 실패했습니다.";
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className="flex flex-1 items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">회원가입</CardTitle>
          <CardDescription>Rental Commerce에 가입하세요</CardDescription>
        </CardHeader>

        <CardContent>
          <SignupForm
            onSubmit={handleSignup}
            isLoading={isLoading}
            serverError={serverError}
          />
        </CardContent>

        <CardFooter className="justify-center">
          <p className="text-sm text-muted-foreground">
            이미 계정이 있으신가요?{" "}
            <Link
              href="/login"
              className="text-primary underline underline-offset-4"
            >
              로그인
            </Link>
          </p>
        </CardFooter>
      </Card>
    </main>
  );
}
