"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getMyDisputesApi } from "@/lib/api/dispute";
import { DisputeStatusBadge } from "@/components/dispute/DisputeStatusBadge";
import type { DisputeStatus } from "@/lib/api/types";

// ========================================
// 마이페이지 분쟁 목록 페이지 (FE-450)
// ========================================

const STATUS_FILTERS: { label: string; value: DisputeStatus | undefined }[] = [
  { label: "전체", value: undefined },
  { label: "오픈", value: "OPEN" },
  { label: "검토 중", value: "UNDER_REVIEW" },
  { label: "해결됨", value: "RESOLVED_REFUND" },
  { label: "취소됨", value: "CANCELLED" },
];

const REASON_LABELS: Record<string, string> = {
  DAMAGED: "파손",
  NOT_RETURNED: "미반납",
  LATE_RETURN: "늦은 반납",
  WRONG_ITEM: "잘못된 상품",
  OTHER: "기타",
};

export default function DisputeListPage() {
  const router = useRouter();
  const [statusFilter, setStatusFilter] = useState<DisputeStatus | undefined>(
    undefined
  );

  const { data, isLoading, isError } = useQuery({
    queryKey: ["my-disputes", statusFilter],
    queryFn: () =>
      getMyDisputesApi({ status: statusFilter, page: 0, size: 20 }),
  });

  const disputes = data?.data?.content ?? [];

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-4 flex items-center gap-3">
          <button
            onClick={() => router.back()}
            className="p-1 rounded-full hover:bg-gray-100"
            aria-label="뒤로가기"
          >
            <svg
              className="w-5 h-5 text-gray-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>
          <h1 className="text-lg font-semibold text-gray-900">내 분쟁 목록</h1>
        </div>
      </header>

      {/* 상태 필터 */}
      <div className="bg-white border-b border-gray-100">
        <div className="max-w-lg mx-auto px-4 py-2 flex gap-2 overflow-x-auto">
          {STATUS_FILTERS.map(({ label, value }) => (
            <button
              key={label}
              onClick={() => setStatusFilter(value)}
              className={`whitespace-nowrap px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                statusFilter === value
                  ? "bg-blue-600 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="max-w-lg mx-auto px-4 py-4">
        {isLoading && (
          <div data-testid="loading-state" className="space-y-3">
            {[...Array(3)].map((_, i) => (
              <div
                key={i}
                className="h-24 bg-gray-200 rounded-xl animate-pulse"
              />
            ))}
          </div>
        )}

        {isError && (
          <div
            data-testid="error-state"
            className="text-center py-12 text-sm text-gray-500"
          >
            분쟁 목록을 불러올 수 없습니다.
          </div>
        )}

        {!isLoading && !isError && disputes.length === 0 && (
          <div className="text-center py-12 text-sm text-gray-500">
            분쟁 내역이 없습니다.
          </div>
        )}

        {!isLoading &&
          disputes.map((dispute) => (
            <Link
              key={dispute.id}
              href={`/disputes/${dispute.id}`}
              className="block bg-white rounded-xl p-4 shadow-sm mb-3 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between gap-2 mb-2">
                <span className="text-sm font-medium text-gray-900">
                  대여 #{dispute.rentalId} —{" "}
                  {REASON_LABELS[dispute.reason] ?? dispute.reason}
                </span>
                <DisputeStatusBadge status={dispute.status} />
              </div>
              <p className="text-sm text-gray-500 line-clamp-2">
                {dispute.description}
              </p>
              <p className="text-xs text-gray-400 mt-2">
                {new Date(dispute.createdAt).toLocaleDateString("ko-KR")}
              </p>
            </Link>
          ))}
      </div>
    </div>
  );
}
