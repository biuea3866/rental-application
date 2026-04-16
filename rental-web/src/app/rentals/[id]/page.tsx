"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { toast } from "sonner";
import { RentalStatusTimeline } from "@/components/rental/RentalStatusTimeline";
import { Button } from "@/components/ui/button";
import {
  getRentalDetailApi,
  approveRentalApi,
  rejectRentalApi,
  cancelRentalApi,
  startRentalApi,
  returnRentalApi,
} from "@/lib/api/rental";
import type { Rental } from "@/lib/api/types";

// ========================================
// 대여 상세 페이지
// 경로: /rentals/{id}
// ========================================

// 현재 로그인 유저 ID (실제 환경에서는 auth store에서)
const MOCK_CURRENT_USER_ID = 2;

export default function RentalDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const [rental, setRental] = useState<Rental | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        setIsLoading(true);
        const res = await getRentalDetailApi(id);
        setRental(res.data);
      } catch {
        setError("대여 정보를 불러올 수 없습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    load();
  }, [id]);

  const isRenter = rental?.renterId === MOCK_CURRENT_USER_ID;
  const isLender = rental?.lenderId === MOCK_CURRENT_USER_ID;

  const handleApprove = async () => {
    if (!rental) return;
    setActionLoading(true);
    try {
      const res = await approveRentalApi(rental.id);
      setRental(res.data);
      toast.success("대여를 승인했습니다.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "승인에 실패했습니다.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!rental || rejectReason.length < 10) {
      toast.error("거절 사유를 10자 이상 입력해 주세요.");
      return;
    }
    setActionLoading(true);
    try {
      const res = await rejectRentalApi(rental.id, { reason: rejectReason });
      setRental(res.data);
      setShowRejectModal(false);
      toast.success("대여를 거절했습니다.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "거절에 실패했습니다.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!rental) return;
    setActionLoading(true);
    try {
      const res = await cancelRentalApi(rental.id);
      setRental(res.data);
      toast.success("대여를 취소했습니다.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "취소에 실패했습니다.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleStart = async () => {
    if (!rental) return;
    setActionLoading(true);
    try {
      const res = await startRentalApi(rental.id);
      setRental(res.data);
      toast.success("배송을 시작했습니다.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "배송 시작에 실패했습니다."
      );
    } finally {
      setActionLoading(false);
    }
  };

  const handleReturn = async () => {
    if (!rental) return;
    setActionLoading(true);
    try {
      const res = await returnRentalApi(rental.id);
      setRental(res.data);
      toast.success("반납 처리가 완료되었습니다. 이용해 주셔서 감사합니다.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "반납 처리에 실패했습니다."
      );
    } finally {
      setActionLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-muted-foreground">불러오는 중...</p>
      </div>
    );
  }

  if (error || !rental) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4">
        <p className="text-destructive">{error ?? "대여 정보를 찾을 수 없습니다."}</p>
        <Link href="/my-rentals" className="text-primary underline text-sm">
          내 대여 목록으로
        </Link>
      </div>
    );
  }

  const startDateStr = rental.startDate.slice(0, 10);
  const endDateStr = rental.endDate.slice(0, 10);

  return (
    <div className="max-w-lg mx-auto px-4 pb-8">
      {/* 헤더 */}
      <div className="flex items-center gap-2 py-4 sticky top-0 bg-background z-10">
        <button onClick={() => router.back()} aria-label="뒤로가기">
          <ChevronLeft className="size-5" />
        </button>
        <h1 className="text-lg font-semibold flex-1 text-center pr-5">
          대여 상세
        </h1>
      </div>

      {/* 상태 타임라인 */}
      <section className="mb-6 p-4 rounded-lg border">
        <RentalStatusTimeline
          status={rental.status}
          dates={{
            requestedAt: rental.requestedAt,
            approvedAt: rental.approvedAt,
            paidAt: rental.paidAt,
            startedAt: rental.startedAt,
            returnedAt: rental.returnedAt,
            cancelledAt: rental.cancelledAt,
          }}
        />
      </section>

      {/* 상품 정보 */}
      <section className="mb-4 p-4 rounded-lg border">
        <h2 className="font-semibold text-sm text-muted-foreground mb-2">
          상품 정보
        </h2>
        <div className="flex items-center gap-3">
          <div className="w-14 h-14 rounded-md overflow-hidden bg-muted shrink-0">
            {rental.productImageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={rental.productImageUrl}
                alt={rental.productName}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
                없음
              </div>
            )}
          </div>
          <p className="font-semibold">{rental.productName}</p>
        </div>
      </section>

      {/* 대여 정보 */}
      <section className="mb-4 p-4 rounded-lg border space-y-1.5">
        <h2 className="font-semibold text-sm text-muted-foreground mb-2">
          대여 정보
        </h2>
        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">기간</span>
          <span>
            {startDateStr} ~ {endDateStr}
          </span>
        </div>
        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">배송지</span>
          <span className="text-right max-w-[60%]">
            {rental.deliveryInfo.address} {rental.deliveryInfo.addressDetail}
          </span>
        </div>
        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">수령인</span>
          <span>
            {rental.deliveryInfo.recipientName} /{" "}
            {rental.deliveryInfo.recipientPhone}
          </span>
        </div>
      </section>

      {/* 결제 정보 (PAID 이후) */}
      {rental.payment && (
        <section className="mb-4 p-4 rounded-lg border space-y-1.5">
          <h2 className="font-semibold text-sm text-muted-foreground mb-2">
            결제 정보
          </h2>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">결제 금액</span>
            <span className="font-semibold">
              {rental.payment.amount.toLocaleString()}원
            </span>
          </div>
          {rental.payment.paymentMethod && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">결제 수단</span>
              <span>{rental.payment.paymentMethod}</span>
            </div>
          )}
          {rental.payment.paidAt && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">결제일</span>
              <span>{new Date(rental.payment.paidAt).toLocaleString("ko-KR")}</span>
            </div>
          )}
        </section>
      )}

      {/* 상태별 액션 버튼 */}
      <div className="space-y-2 mt-4">
        {/* 등록자: REQUESTED → 승인/거절 */}
        {isLender && rental.status === "REQUESTED" && (
          <div className="grid grid-cols-2 gap-2">
            <Button
              variant="outline"
              onClick={() => setShowRejectModal(true)}
              disabled={actionLoading}
              data-testid="reject-button"
            >
              거절
            </Button>
            <Button
              onClick={handleApprove}
              disabled={actionLoading}
              data-testid="approve-button"
            >
              승인
            </Button>
          </div>
        )}

        {/* 등록자: PAID → 배송 시작 */}
        {isLender && rental.status === "PAID" && (
          <Button
            className="w-full"
            onClick={handleStart}
            disabled={actionLoading}
            data-testid="start-button"
          >
            배송 시작
          </Button>
        )}

        {/* 대여자: APPROVED → 결제하기 */}
        {isRenter && rental.status === "APPROVED" && (
          <Button
            className="w-full"
            onClick={() => router.push(`/rentals/${rental.id}/payment`)}
            data-testid="go-payment-button"
          >
            결제하기
          </Button>
        )}

        {/* 대여자: IN_USE → 반납 처리 */}
        {isRenter && rental.status === "IN_USE" && (
          <Button
            className="w-full"
            onClick={handleReturn}
            disabled={actionLoading}
            data-testid="return-button"
          >
            반납 처리
          </Button>
        )}

        {/* 취소 버튼 (REQUESTED | APPROVED) */}
        {["REQUESTED", "APPROVED"].includes(rental.status) && (
          <Button
            variant="outline"
            className="w-full"
            onClick={handleCancel}
            disabled={actionLoading}
            data-testid="cancel-button"
          >
            취소하기
          </Button>
        )}
      </div>

      {/* 거절 모달 */}
      {showRejectModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 px-4">
          <div className="bg-background rounded-xl p-6 w-full max-w-sm space-y-4">
            <h2 className="font-semibold">거절 사유 입력</h2>
            <textarea
              className="w-full border rounded-lg p-3 text-sm resize-none h-24"
              placeholder="거절 사유를 10자 이상 입력해 주세요."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              data-testid="reject-reason-input"
            />
            <div className="grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                onClick={() => setShowRejectModal(false)}
              >
                닫기
              </Button>
              <Button
                onClick={handleReject}
                disabled={actionLoading || rejectReason.length < 10}
                data-testid="confirm-reject-button"
              >
                거절 확인
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
