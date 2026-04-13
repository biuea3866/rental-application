"use client";

import { useEffect, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuthStore } from "@/stores/auth-store";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { AuthTokens, SocialProvider, User } from "@/lib/api/types";

// ========================================
// 소셜 로그인 콜백 페이지
// ========================================

function SocialCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const storeLogin = useAuthStore((s) => s.login);
  const calledRef = useRef(false);

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    const code = searchParams.get("code");
    const state = searchParams.get("state") as SocialProvider | null;
    const error = searchParams.get("error");

    if (error || !code || !state) {
      router.replace("/login?error=social_login_failed");
      return;
    }

    const redirectUri = `${window.location.origin}/login/social/callback`;
    const apiClient = getApiClient();

    apiClient
      .post<AuthTokens>(
        ENDPOINTS.AUTH.SOCIAL_LOGIN,
        { provider: state, code, redirectUri },
        { requiresAuth: false }
      )
      .then(async (tokenResponse) => {
        const userResponse = await apiClient.get<User>(ENDPOINTS.AUTH.ME);
        storeLogin(userResponse.data, tokenResponse.data);
        router.replace("/");
      })
      .catch(() => {
        router.replace("/login?error=social_login_failed");
      });
  }, [searchParams, router, storeLogin]);

  return (
    <main className="flex flex-1 items-center justify-center p-6">
      <div className="text-center space-y-2">
        <div
          className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
          role="status"
          aria-label="로그인 처리 중"
        />
        <p className="text-sm text-muted-foreground">로그인 처리 중...</p>
      </div>
    </main>
  );
}

export default function SocialCallbackPage() {
  return (
    <Suspense
      fallback={
        <main className="flex flex-1 items-center justify-center p-6">
          <div className="text-center space-y-2">
            <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            <p className="text-sm text-muted-foreground">로딩 중...</p>
          </div>
        </main>
      }
    >
      <SocialCallbackContent />
    </Suspense>
  );
}
