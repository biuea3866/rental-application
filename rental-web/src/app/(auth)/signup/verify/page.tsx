"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "@/components/ui/card";
import { PhoneVerification } from "@/components/auth/PhoneVerification";
import { useAuthStore } from "@/stores/auth-store";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { AuthTokens, User } from "@/lib/api/types";

// ========================================
// 전화번호 인증 페이지
// ========================================

function VerifyPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const phone = searchParams.get("phone") ?? "";
  const storeLogin = useAuthStore((s) => s.login);
  const isLoading = useAuthStore((s) => s.isLoading);

  if (!phone) {
    return (
      <main className="flex flex-1 items-center justify-center p-6">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center text-sm text-muted-foreground">
            전화번호 정보가 없습니다.{" "}
            <Link href="/signup" className="text-primary underline">
              회원가입으로 돌아가기
            </Link>
          </CardContent>
        </Card>
      </main>
    );
  }

  const handleVerify = async (code: string) => {
    const apiClient = getApiClient();
    try {
      const response = await apiClient.post<AuthTokens>(
        ENDPOINTS.AUTH.VERIFY_PHONE,
        { phone, code },
        { requiresAuth: false }
      );

      // 인증 완료 후 토큰 발급 → 홈으로
      const userResponse = await apiClient.get<User>(ENDPOINTS.AUTH.ME);
      storeLogin(userResponse.data, response.data);
      router.push("/");
      return { success: true };
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "인증에 실패했습니다.";
      return { success: false, error: message };
    }
  };

  const handleResend = async () => {
    const apiClient = getApiClient();
    try {
      await apiClient.post(
        ENDPOINTS.AUTH.SIGNUP,
        { phone },
        { requiresAuth: false }
      );
      return { success: true };
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "재전송에 실패했습니다.";
      return { success: false, error: message };
    }
  };

  return (
    <main className="flex flex-1 items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">휴대폰 인증</CardTitle>
          <CardDescription>
            입력하신 번호로 인증코드를 발송했습니다.
          </CardDescription>
        </CardHeader>

        <CardContent>
          <PhoneVerification
            phone={phone}
            onVerify={handleVerify}
            onResend={handleResend}
            isLoading={isLoading}
          />
        </CardContent>

        <CardFooter className="justify-center">
          <p className="text-sm text-muted-foreground">
            <Link
              href="/signup"
              className="text-primary underline underline-offset-4"
            >
              회원가입으로 돌아가기
            </Link>
          </p>
        </CardFooter>
      </Card>
    </main>
  );
}

export default function VerifyPage() {
  return (
    <Suspense fallback={<div className="flex flex-1 items-center justify-center">로딩 중...</div>}>
      <VerifyPageContent />
    </Suspense>
  );
}
