"use client";

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
import { LoginForm } from "@/components/auth/LoginForm";
import { SocialLoginButtons } from "@/components/auth/SocialLoginButtons";
import { useAuth } from "@/hooks/use-auth";
import { useAuthStore } from "@/stores/auth-store";
import type { LoginFormValues } from "@/components/auth/LoginForm";
import type { SocialProvider } from "@/lib/api/types";

// ========================================
// 로그인 페이지
// ========================================

export default function LoginPage() {
  const router = useRouter();
  const { login, isLoading, error } = useAuth();

  const handleLogin = async (values: LoginFormValues) => {
    const result = await login({ email: values.email, password: values.password });
    if (result.success) {
      // role에 따라 적절한 페이지로 이동
      const currentUser = useAuthStore.getState().user;
      if (currentUser?.role === "LENDER") {
        router.push("/lender");
      } else if (currentUser?.role === "RENTER") {
        router.push("/products");
      } else {
        router.push("/");
      }
    }
  };

  const handleSocialLogin = async (provider: SocialProvider) => {
    const redirectUri = `${window.location.origin}/login/social/callback`;
    const socialLoginUrls: Record<SocialProvider, string> = {
      KAKAO: `https://kauth.kakao.com/oauth/authorize?client_id=${process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&state=${provider}`,
      NAVER: `https://nid.naver.com/oauth2.0/authorize?client_id=${process.env.NEXT_PUBLIC_NAVER_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&state=${provider}`,
    };

    window.location.href = socialLoginUrls[provider];
  };

  return (
    <main className="flex flex-1 items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">로그인</CardTitle>
          <CardDescription>Rental Commerce에 로그인하세요</CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          <LoginForm
            onSubmit={handleLogin}
            isLoading={isLoading}
            serverError={error}
          />

          <SocialLoginButtons
            onSocialLogin={handleSocialLogin}
            isLoading={isLoading}
          />
        </CardContent>

        <CardFooter className="justify-center">
          <p className="text-sm text-muted-foreground">
            계정이 없으신가요?{" "}
            <Link href="/signup" className="text-primary underline underline-offset-4">
              회원가입
            </Link>
          </p>
        </CardFooter>
      </Card>
    </main>
  );
}
