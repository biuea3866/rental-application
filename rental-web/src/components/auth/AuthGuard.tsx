"use client";

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// 보호된 경로 목록
// ========================================

/** 로그인이 필요한 경로 접두사 */
const AUTH_REQUIRED_PATHS = ["/mypage", "/lender", "/products"];

/** LENDER 전용 경로 접두사 */
const LENDER_ONLY_PATHS = ["/lender"];

/** RENTER 전용 경로 접두사 */
const RENTER_ONLY_PATHS = ["/products"];

// ========================================
// AuthGuard 컴포넌트
// ========================================

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { user, isAuthenticated } = useAuthStore();

  const requiresAuth = AUTH_REQUIRED_PATHS.some((p) => pathname.startsWith(p));
  const isLenderOnly = LENDER_ONLY_PATHS.some((p) => pathname.startsWith(p));
  const isRenterOnly = RENTER_ONLY_PATHS.some((p) => pathname.startsWith(p));

  useEffect(() => {
    if (!requiresAuth) return;

    // 비로그인 → 로그인 페이지로
    if (!isAuthenticated || !user) {
      router.replace("/login");
      return;
    }

    // LENDER 전용 경로에 RENTER가 접근
    if (isLenderOnly && user.role === "RENTER") {
      router.replace("/products");
      return;
    }

    // RENTER 전용 경로에 LENDER가 접근
    if (isRenterOnly && user.role === "LENDER") {
      router.replace("/lender");
      return;
    }
  }, [isAuthenticated, user, pathname, router, requiresAuth, isLenderOnly, isRenterOnly]);

  return <>{children}</>;
}
