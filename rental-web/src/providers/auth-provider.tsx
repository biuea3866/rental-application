"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/stores/auth-store";
import { getApiMode } from "@/lib/api/client";
import { registerStubHandlers } from "@/mocks/handlers";

// ========================================
// Auth Provider
// ========================================

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { checkAuth, isLoading } = useAuthStore();

  useEffect(() => {
    // 스텁 모드인 경우 핸들러 등록
    if (getApiMode() === "stub") {
      registerStubHandlers();
    }

    // 인증 상태 확인
    checkAuth();
  }, [checkAuth]);

  // 초기 인증 확인 중에는 로딩 표시
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return <>{children}</>;
}
