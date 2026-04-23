"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { adminGetDisputesApi } from "@/lib/api/dispute";
import { DisputeStatusBadge } from "@/components/dispute/DisputeStatusBadge";
import type { DisputeStatus } from "@/lib/api/types";

// ========================================
// 관리자 분쟁 목록 페이지 (FE-451)
// 상태 필터 + 날짜 정렬
// ========================================

const STATUS_FILTERS: { label: string; value: DisputeStatus | undefined }[] = [
  { label: "전체", value: undefined },
  { label: "OPEN", value: "OPEN" },
  { label: "검토 중", value: "UNDER_REVIEW" },
  { label: "전액환불", value: "RESOLVED_REFUND" },
  { label: "부분환불", value: "RESOLVED_PARTIAL" },
  { label: "기각", value: "RESOLVED_REJECTED" },
  { label: "취소됨", value: "CANCELLED" },
];

const REASON_LABELS: Record<string, string> = {
  DAMAGED: "파손",
  NOT_RETURNED: "미반납",
  LATE_RETURN: "늦은 반납",
  WRONG_ITEM: "잘못된 상품",
  OTHER: "기타",
};

export default function AdminDisputeListPage() {
  const [statusFilter, setStatusFilter] = useState<DisputeStatus | undefined>(undefined);
  const [sortDir, setSortDir] = useState<"desc" | "asc">("desc");

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["admin-disputes", statusFilter, sortDir],
    queryFn: () =>
      adminGetDisputesApi({
        status: statusFilter,
        page: 0,
        size: 50,
        sort: `createdAt,${sortDir}`,
      }),
  });

  const disputes = data?.data?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">분쟁 관리</h1>
        <button
          onClick={() => refetch()}
          className="px-3 py-1.5 text-sm text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          새로고침
        </button>
      </div>

      {/* 필터 + 정렬 */}
      <div className="flex flex-wrap items-center gap-3">
        {/* 상태 필터 */}
        <div className="flex flex-wrap gap-2">
          {STATUS_FILTERS.map(({ label, value }) => (
            <button
              key={label}
              onClick={() => setStatusFilter(value)}
              className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                statusFilter === value
                  ? "bg-blue-600 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* 날짜 정렬 */}
        <select
          value={sortDir}
          onChange={(e) => setSortDir(e.target.value as "desc" | "asc")}
          className="ml-auto border border-gray-300 rounded-lg px-3 py-1.5 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-300"
          aria-label="날짜 정렬"
        >
          <option value="desc">최신순</option>
          <option value="asc">오래된 순</option>
        </select>
      </div>

      {/* 분쟁 목록 테이블 */}
      {isLoading && (
        <div data-testid="disputes-loading" className="space-y-2">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 bg-gray-200 rounded-lg animate-pulse" />
          ))}
        </div>
      )}

      {isError && (
        <div data-testid="disputes-error" className="text-center py-12 text-sm text-gray-500">
          분쟁 목록을 불러올 수 없습니다.
        </div>
      )}

      {!isLoading && !isError && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          {disputes.length === 0 ? (
            <div className="text-center py-12 text-sm text-gray-500">
              분쟁 내역이 없습니다.
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">대여 #</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">사유</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">상태</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">오픈일</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {disputes.map((dispute) => (
                  <tr key={dispute.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-900">#{dispute.id}</td>
                    <td className="px-4 py-3 text-gray-600">
                      <Link href={`/rentals/${dispute.rentalId}`} className="text-blue-600 hover:underline">
                        #{dispute.rentalId}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-gray-700">
                      {REASON_LABELS[dispute.reason] ?? dispute.reason}
                    </td>
                    <td className="px-4 py-3">
                      <DisputeStatusBadge status={dispute.status} />
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {new Date(dispute.createdAt).toLocaleDateString("ko-KR")}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Link
                        href={`/admin/disputes/${dispute.id}`}
                        className="text-blue-600 hover:underline text-xs font-medium"
                      >
                        상세
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
