"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// 관리자 레이아웃 (어드민 롤 체크)
// ========================================

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isLoading = useAuthStore((s) => s.isLoading);

  useEffect(() => {
    if (isLoading) return;

    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }

    // 관리자 역할이 아니면 홈으로 리다이렉트
    if (user?.role !== "ADMIN") {
      router.replace("/");
    }
  }, [isAuthenticated, isLoading, user, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-sm text-gray-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 관리자 사이드바 / 헤더 */}
      <nav className="bg-gray-900 text-white">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center gap-4">
          <span className="text-sm font-bold tracking-wide text-yellow-400">
            ADMIN
          </span>
          <Link href="/admin" className="text-sm text-gray-300 hover:text-white">
            대시보드
          </Link>
          <Link href="/admin/disputes" className="text-sm text-gray-300 hover:text-white">
            분쟁 관리
          </Link>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 py-6">{children}</main>
    </div>
  );
}
