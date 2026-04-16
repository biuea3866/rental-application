"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { RentalCard } from "@/components/rental/RentalCard";
import { getMyRentalsApi } from "@/lib/api/rental";
import type { Rental, RentalStatus } from "@/lib/api/types";

// ========================================
// 내 대여 목록 페이지
// 경로: /my-rentals
// ========================================

type RoleTab = "renter" | "lender";

const STATUS_FILTER_TABS: {
  label: string;
  statuses: RentalStatus[] | null;
}[] = [
  { label: "전체", statuses: null },
  { label: "대여 중", statuses: ["IN_USE", "PAID", "APPROVED", "REQUESTED"] },
  { label: "완료", statuses: ["RETURNED"] },
  { label: "취소됨", statuses: ["CANCELLED"] },
];

export default function MyRentalsPage() {
  const [roleTab, setRoleTab] = useState<RoleTab>("renter");
  const [statusTab, setStatusTab] = useState(0);
  const [rentals, setRentals] = useState<Rental[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setIsLoading(true);
        const res = await getMyRentalsApi({ role: roleTab });
        setRentals(res.data.content);
      } catch {
        setError("대여 목록을 불러올 수 없습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    load();
  }, [roleTab]);

  const filteredRentals = rentals.filter((r) => {
    const statusFilter = STATUS_FILTER_TABS[statusTab]?.statuses;
    if (!statusFilter) return true;
    return statusFilter.includes(r.status);
  });

  return (
    <div className="max-w-lg mx-auto px-4 pb-8">
      {/* 헤더 */}
      <div className="flex items-center gap-2 py-4 sticky top-0 bg-background z-10">
        <Link href="/" aria-label="뒤로가기">
          <ChevronLeft className="size-5" />
        </Link>
        <h1 className="text-lg font-semibold flex-1 text-center pr-5">
          내 대여 목록
        </h1>
      </div>

      {/* 역할 탭 (대여자 / 등록자) */}
      <div className="flex gap-1 mb-4 rounded-lg bg-muted p-1">
        <button
          onClick={() => setRoleTab("renter")}
          className={`flex-1 rounded-md py-2 text-sm font-medium transition-colors
            ${roleTab === "renter" ? "bg-background shadow-sm" : "text-muted-foreground"}`}
          data-testid="tab-renter"
        >
          대여자로서
        </button>
        <button
          onClick={() => setRoleTab("lender")}
          className={`flex-1 rounded-md py-2 text-sm font-medium transition-colors
            ${roleTab === "lender" ? "bg-background shadow-sm" : "text-muted-foreground"}`}
          data-testid="tab-lender"
        >
          등록자로서
        </button>
      </div>

      {/* 상태 필터 탭 */}
      <div className="flex gap-1 mb-4 overflow-x-auto pb-1">
        {STATUS_FILTER_TABS.map((tab, idx) => (
          <button
            key={tab.label}
            onClick={() => setStatusTab(idx)}
            className={`shrink-0 rounded-full px-3 py-1 text-xs font-medium transition-colors
              ${statusTab === idx ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"}`}
            data-testid={`status-filter-${idx}`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 목록 */}
      {isLoading ? (
        <div
          data-testid="rentals-loading"
          className="flex items-center justify-center py-16"
        >
          <p className="text-muted-foreground">불러오는 중...</p>
        </div>
      ) : error ? (
        <div className="flex items-center justify-center py-16">
          <p className="text-destructive">{error}</p>
        </div>
      ) : filteredRentals.length === 0 ? (
        <div
          data-testid="rentals-empty"
          className="flex flex-col items-center justify-center py-16 text-center gap-2"
        >
          <p className="text-muted-foreground text-sm">
            {statusTab === 0 ? "대여 내역이 없습니다." : "해당 상태의 대여가 없습니다."}
          </p>
          {roleTab === "renter" && (
            <Link href="/" className="text-primary text-sm underline mt-2">
              상품 둘러보기
            </Link>
          )}
        </div>
      ) : (
        <div
          data-testid="rentals-list"
          className="space-y-3"
        >
          {filteredRentals.map((rental) => (
            <RentalCard key={rental.id} rental={rental} />
          ))}
        </div>
      )}
    </div>
  );
}
