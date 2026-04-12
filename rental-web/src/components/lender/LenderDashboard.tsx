"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { MyProductList } from "./MyProductList";

// ========================================
// LenderDashboard — 등록자 대시보드
// ========================================

interface LenderDashboardProps {
  userName?: string;
}

export function LenderDashboard({ userName }: LenderDashboardProps) {
  return (
    <div className="mx-auto max-w-2xl space-y-6 px-4 py-8">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">
            {userName ? `${userName}님의 대시보드` : "등록자 대시보드"}
          </h1>
          <p className="mt-0.5 text-sm text-muted-foreground">
            내 상품을 관리하고 대여 현황을 확인하세요.
          </p>
        </div>
        <Link href="/lender/products/new">
          <Button>
            <svg
              className="mr-1.5 h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 4v16m8-8H4"
              />
            </svg>
            상품 등록
          </Button>
        </Link>
      </div>

      {/* 내 상품 목록 */}
      <section>
        <h2 className="mb-3 text-base font-semibold">내 상품</h2>
        <MyProductList />
      </section>
    </div>
  );
}
