"use client";

import { use } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getDisputeApi } from "@/lib/api/dispute";
import { DisputeStatusBadge } from "@/components/dispute/DisputeStatusBadge";
import { AdminDisputeActions } from "@/components/dispute/AdminDisputeActions";

// ========================================
// 관리자 분쟁 상세 페이지 (FE-451)
// 증빙 이미지 갤러리 + 당사자 정보 + 대여 링크 + 액션
// ========================================

const REASON_LABELS: Record<string, string> = {
  DAMAGED: "파손",
  NOT_RETURNED: "미반납",
  LATE_RETURN: "늦은 반납",
  WRONG_ITEM: "잘못된 상품",
  OTHER: "기타",
};

interface AdminDisputeDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function AdminDisputeDetailPage({
  params,
}: AdminDisputeDetailPageProps) {
  const { id } = use(params);
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["admin-dispute", id],
    queryFn: () => getDisputeApi(id),
  });

  const dispute = data?.data;

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-32 bg-gray-200 rounded animate-pulse" />
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-28 bg-gray-200 rounded-xl animate-pulse" />
        ))}
      </div>
    );
  }

  if (isError || !dispute) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 mb-4">분쟁 정보를 불러올 수 없습니다.</p>
        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 mr-2"
        >
          다시 시도
        </button>
        <button
          onClick={() => router.back()}
          className="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm hover:bg-gray-50"
        >
          뒤로가기
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => router.back()}
          className="p-2 rounded-lg hover:bg-gray-100"
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
        <h1 className="text-xl font-bold text-gray-900">
          분쟁 #{dispute.id}
        </h1>
        <DisputeStatusBadge status={dispute.status} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 주요 정보 */}
        <div className="lg:col-span-2 space-y-4">
          {/* 분쟁 기본 정보 */}
          <section className="bg-white rounded-xl p-5 border border-gray-200">
            <h2 className="text-sm font-semibold text-gray-700 mb-3">
              분쟁 정보
            </h2>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <div>
                <dt className="text-gray-500 text-xs mb-0.5">사유</dt>
                <dd className="text-gray-900 font-medium">
                  {REASON_LABELS[dispute.reason] ?? dispute.reason}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500 text-xs mb-0.5">관련 대여</dt>
                <dd>
                  <Link
                    href={`/rentals/${dispute.rentalId}`}
                    className="text-blue-600 hover:underline font-medium"
                  >
                    대여 #{dispute.rentalId}
                  </Link>
                </dd>
              </div>
              <div>
                <dt className="text-gray-500 text-xs mb-0.5">신청인 ID</dt>
                <dd className="text-gray-900">#{dispute.openerId}</dd>
              </div>
              <div>
                <dt className="text-gray-500 text-xs mb-0.5">오픈일</dt>
                <dd className="text-gray-900">
                  {new Date(dispute.createdAt).toLocaleString("ko-KR")}
                </dd>
              </div>
              {dispute.resolvedAt && (
                <div>
                  <dt className="text-gray-500 text-xs mb-0.5">해결일</dt>
                  <dd className="text-gray-900">
                    {new Date(dispute.resolvedAt).toLocaleString("ko-KR")}
                  </dd>
                </div>
              )}
              {dispute.refundAmount != null && (
                <div>
                  <dt className="text-gray-500 text-xs mb-0.5">환불 금액</dt>
                  <dd className="text-green-700 font-semibold">
                    {dispute.refundAmount.toLocaleString("ko-KR")}원
                  </dd>
                </div>
              )}
            </dl>
          </section>

          {/* 상세 설명 */}
          <section className="bg-white rounded-xl p-5 border border-gray-200">
            <h2 className="text-sm font-semibold text-gray-700 mb-2">
              상세 설명
            </h2>
            <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
              {dispute.description}
            </p>
          </section>

          {/* 증빙 이미지 갤러리 */}
          {dispute.attachmentUrls.length > 0 && (
            <section className="bg-white rounded-xl p-5 border border-gray-200">
              <h2 className="text-sm font-semibold text-gray-700 mb-3">
                증빙 이미지 ({dispute.attachmentUrls.length}장)
              </h2>
              <div className="grid grid-cols-3 gap-3">
                {dispute.attachmentUrls.map((url, idx) => (
                  <a
                    key={idx}
                    href={url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="aspect-square rounded-lg overflow-hidden bg-gray-100 hover:opacity-90 transition-opacity"
                  >
                    <img
                      src={url}
                      alt={`증빙 이미지 ${idx + 1}`}
                      className="w-full h-full object-cover"
                    />
                  </a>
                ))}
              </div>
            </section>
          )}
        </div>

        {/* 사이드바: 관리자 액션 */}
        <div className="space-y-4">
          <section className="bg-white rounded-xl p-5 border border-gray-200">
            <h2 className="text-sm font-semibold text-gray-700 mb-3">
              관리자 액션
            </h2>
            <AdminDisputeActions
              dispute={dispute}
              onUpdated={() => {
                queryClient.invalidateQueries({
                  queryKey: ["admin-dispute", id],
                });
                queryClient.invalidateQueries({ queryKey: ["admin-disputes"] });
              }}
            />
          </section>
        </div>
      </div>
    </div>
  );
}
