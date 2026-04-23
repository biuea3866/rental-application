"use client";

import { use } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getDisputeApi } from "@/lib/api/dispute";
import { DisputeStatusBadge } from "@/components/dispute/DisputeStatusBadge";
import { DisputeStatusButtons } from "@/components/dispute/DisputeStatusButtons";

// ========================================
// 분쟁 상세 페이지 (FE-450)
// opener 전용 — 403/404 핸들링 포함
// ========================================

const REASON_LABELS: Record<string, string> = {
  DAMAGED: "파손",
  NOT_RETURNED: "미반납",
  LATE_RETURN: "늦은 반납",
  WRONG_ITEM: "잘못된 상품",
  OTHER: "기타",
};

interface DisputeDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function DisputeDetailPage({ params }: DisputeDetailPageProps) {
  const { id } = use(params);
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["dispute", id],
    queryFn: () => getDisputeApi(id),
  });

  const dispute = data?.data;

  if (isLoading) {
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
            <h1 className="text-lg font-semibold text-gray-900">분쟁 상세</h1>
          </div>
        </header>
        <div className="max-w-lg mx-auto px-4 py-6 space-y-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-28 bg-gray-200 rounded-xl animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (isError || !dispute) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center px-4">
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
      </div>
    );
  }

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
          <h1 className="text-lg font-semibold text-gray-900">분쟁 상세</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-4 space-y-4">
        {/* 분쟁 기본 정보 */}
        <section className="bg-white rounded-xl p-4 shadow-sm">
          <div className="flex items-start justify-between mb-3">
            <h2 className="text-sm font-semibold text-gray-700">분쟁 정보</h2>
            <DisputeStatusBadge status={dispute.status} />
          </div>
          <dl className="space-y-2">
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">분쟁 번호</dt>
              <dd className="text-gray-900 font-medium">#{dispute.id}</dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">관련 대여</dt>
              <dd>
                <Link
                  href={`/rentals/${dispute.rentalId}`}
                  className="text-blue-600 hover:underline"
                >
                  대여 #{dispute.rentalId}
                </Link>
              </dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">사유</dt>
              <dd className="text-gray-900">
                {REASON_LABELS[dispute.reason] ?? dispute.reason}
              </dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">오픈일</dt>
              <dd className="text-gray-900">
                {new Date(dispute.createdAt).toLocaleDateString("ko-KR")}
              </dd>
            </div>
            {dispute.resolvedAt && (
              <div className="flex justify-between text-sm">
                <dt className="text-gray-500">해결일</dt>
                <dd className="text-gray-900">
                  {new Date(dispute.resolvedAt).toLocaleDateString("ko-KR")}
                </dd>
              </div>
            )}
            {dispute.refundAmount != null && (
              <div className="flex justify-between text-sm font-semibold border-t border-gray-100 pt-2 mt-2">
                <dt className="text-gray-700">환불 금액</dt>
                <dd className="text-green-700">
                  {dispute.refundAmount.toLocaleString("ko-KR")}원
                </dd>
              </div>
            )}
          </dl>
        </section>

        {/* 상세 설명 */}
        <section className="bg-white rounded-xl p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-gray-700 mb-2">
            상세 설명
          </h2>
          <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
            {dispute.description}
          </p>
        </section>

        {/* 증빙 이미지 갤러리 */}
        {dispute.attachmentUrls.length > 0 && (
          <section className="bg-white rounded-xl p-4 shadow-sm">
            <h2 className="text-sm font-semibold text-gray-700 mb-3">
              증빙 이미지
            </h2>
            <div className="grid grid-cols-3 gap-2">
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

        {/* 액션 버튼 (OPEN 상태에서만 취소 가능) */}
        <section className="bg-white rounded-xl p-4 shadow-sm">
          <DisputeStatusButtons
            dispute={dispute}
            onCancelled={() => {
              queryClient.invalidateQueries({ queryKey: ["dispute", id] });
              queryClient.invalidateQueries({ queryKey: ["my-disputes"] });
            }}
          />
        </section>
      </div>
    </div>
  );
}
