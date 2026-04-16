"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { RentalStatusTimeline } from "@/components/rental/RentalStatusTimeline";
import { RentalActionButtons } from "@/components/rental/RentalActionButtons";
import { getRentalDetailApi } from "@/lib/api/rental";

// ========================================
// 대여 상세 페이지
// PRD-002: 5.4 대여 상세 페이지 — 상태 타임라인 + 액션 버튼
// RC-FE-215
// ========================================

/** 로그인 유저 ID를 가져오는 임시 헬퍼 (실제로는 auth store에서 가져옴) */
function getCurrentUserId(): number {
  if (typeof window === "undefined") return 0;
  try {
    const stored = localStorage.getItem("rental_user_id");
    return stored ? parseInt(stored, 10) : 3; // 기본값: 대여자(id=3)
  } catch {
    return 3;
  }
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

function formatAmount(amount: number): string {
  return amount.toLocaleString("ko-KR") + "원";
}

interface RentalDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function RentalDetailPage({ params }: RentalDetailPageProps) {
  const { id } = use(params);
  const router = useRouter();
  const currentUserId = getCurrentUserId();

  const {
    data,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["rental-detail", id],
    queryFn: () => getRentalDetailApi(id),
  });

  const rental = data?.data;

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
              <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <h1 className="text-lg font-semibold text-gray-900">대여 상세</h1>
          </div>
        </header>
        <div data-testid="loading-state" className="max-w-lg mx-auto px-4 py-6 space-y-4">
          {[...Array(4)].map((_, index) => (
            <div key={index} className="h-32 bg-gray-200 rounded-lg animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (isError || !rental) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div data-testid="error-state" className="text-center px-4">
          <p className="text-gray-500 mb-4">대여 정보를 불러올 수 없습니다.</p>
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

  const rentalDays = Math.ceil(
    (new Date(rental.endDate).getTime() - new Date(rental.startDate).getTime()) /
      (1000 * 60 * 60 * 24)
  );

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-4 flex items-center gap-3">
          <button
            onClick={() => router.back()}
            className="p-1 rounded-full hover:bg-gray-100"
            aria-label="뒤로가기"
          >
            <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 className="text-lg font-semibold text-gray-900">대여 상세</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-4 space-y-4">
        {/* 상태 타임라인 섹션 */}
        <section className="bg-white rounded-xl p-4 shadow-sm" data-testid="timeline-section">
          <RentalStatusTimeline rental={rental} />
        </section>

        {/* 상품 정보 */}
        <section className="bg-white rounded-xl p-4 shadow-sm" data-testid="product-section">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">상품 정보</h2>
          <div className="flex items-center gap-3">
            <div className="w-16 h-16 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
              {rental.product.thumbnailUrl ? (
                <img
                  src={rental.product.thumbnailUrl}
                  alt={rental.product.name}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">
                  이미지 없음
                </div>
              )}
            </div>
            <div>
              <p className="font-medium text-gray-900">{rental.product.name}</p>
              <p className="text-sm text-gray-500 mt-0.5">{rental.product.category}</p>
            </div>
          </div>
        </section>

        {/* 대여 정보 */}
        <section className="bg-white rounded-xl p-4 shadow-sm" data-testid="rental-info-section">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">대여 정보</h2>
          <dl className="space-y-2">
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">대여 기간</dt>
              <dd className="text-gray-900 font-medium">
                {formatDate(rental.startDate)} ~ {formatDate(rental.endDate)}{" "}
                <span className="text-gray-500">({rentalDays}일)</span>
              </dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">대여자</dt>
              <dd className="text-gray-900">{rental.renter.name}</dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">등록자</dt>
              <dd className="text-gray-900">{rental.lender.name}</dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">배송지</dt>
              <dd className="text-gray-900 text-right max-w-[200px]">
                {rental.deliveryInfo.addressLine1}
                {rental.deliveryInfo.addressLine2 &&
                  ` ${rental.deliveryInfo.addressLine2}`}
              </dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">수령인</dt>
              <dd className="text-gray-900">
                {rental.deliveryInfo.recipientName} /{" "}
                {rental.deliveryInfo.recipientPhone}
              </dd>
            </div>
          </dl>
        </section>

        {/* 요금 정보 */}
        <section className="bg-white rounded-xl p-4 shadow-sm" data-testid="price-section">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">요금 정보</h2>
          <dl className="space-y-2">
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">대여료</dt>
              <dd className="text-gray-900">{formatAmount(rental.totalAmount)}</dd>
            </div>
            <div className="flex justify-between text-sm">
              <dt className="text-gray-500">보증금</dt>
              <dd className="text-gray-900">{formatAmount(rental.depositAmount)}</dd>
            </div>
            <div className="flex justify-between text-sm font-semibold border-t border-gray-100 pt-2 mt-2">
              <dt className="text-gray-700">결제 예정</dt>
              <dd className="text-gray-900">
                {formatAmount(rental.totalAmount + rental.depositAmount)}
              </dd>
            </div>
          </dl>
        </section>

        {/* 결제 정보 (PAID 이후) */}
        {rental.payment && (
          <section
            className="bg-white rounded-xl p-4 shadow-sm"
            data-testid="payment-section"
          >
            <h2 className="text-sm font-semibold text-gray-700 mb-3">결제 정보</h2>
            <dl className="space-y-2">
              <div className="flex justify-between text-sm">
                <dt className="text-gray-500">결제 금액</dt>
                <dd className="text-gray-900 font-medium">
                  {formatAmount(rental.payment.amount)}
                </dd>
              </div>
              <div className="flex justify-between text-sm">
                <dt className="text-gray-500">결제 수단</dt>
                <dd className="text-gray-900">{rental.payment.paymentMethod}</dd>
              </div>
              <div className="flex justify-between text-sm">
                <dt className="text-gray-500">결제일</dt>
                <dd className="text-gray-900">
                  {new Date(rental.payment.paidAt).toLocaleString("ko-KR")}
                </dd>
              </div>
            </dl>
          </section>
        )}

        {/* 액션 버튼 */}
        <section className="bg-white rounded-xl p-4 shadow-sm" data-testid="action-section">
          <RentalActionButtons
            rental={rental}
            currentUserId={currentUserId}
            onActionComplete={() => refetch()}
          />
        </section>
      </div>
    </div>
  );
}
