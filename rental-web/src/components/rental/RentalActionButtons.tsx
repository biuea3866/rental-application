"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import type { RentalDetail } from "@/lib/api/types";
import {
  approveRentalApi,
  rejectRentalApi,
  startRentalApi,
  returnRentalApi,
  cancelRentalApi,
} from "@/lib/api/rental";

// ========================================
// 대여 액션 버튼 컴포넌트
// PRD-002: 5.4 대여 상세 페이지 — 상태별 액션 버튼
// ========================================

interface RentalActionButtonsProps {
  rental: RentalDetail;
  currentUserId: number;
  onActionComplete?: () => void;
}

export function RentalActionButtons({
  rental,
  currentUserId,
  onActionComplete,
}: RentalActionButtonsProps) {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [cancelReason, setCancelReason] = useState("");

  const isRenter = rental.renter.userId === currentUserId;
  const isLender = rental.lender.userId === currentUserId;

  async function handleApprove() {
    setIsLoading(true);
    try {
      await approveRentalApi(rental.rentalId);
      toast.success("대여 요청을 승인했습니다.");
      onActionComplete?.();
      router.refresh();
    } catch {
      toast.error("승인 처리 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleReject() {
    if (rejectReason.length < 10) {
      toast.error("거절 사유는 최소 10자 이상 입력해주세요.");
      return;
    }
    setIsLoading(true);
    try {
      await rejectRentalApi(rental.rentalId, { reason: rejectReason });
      toast.success("대여 요청을 거절했습니다.");
      setShowRejectModal(false);
      onActionComplete?.();
      router.refresh();
    } catch {
      toast.error("거절 처리 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleStart() {
    setIsLoading(true);
    try {
      await startRentalApi(rental.rentalId);
      toast.success("배송을 시작했습니다.");
      onActionComplete?.();
      router.refresh();
    } catch {
      toast.error("배송 시작 처리 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleReturn() {
    setIsLoading(true);
    try {
      await returnRentalApi(rental.rentalId);
      toast.success("반납 처리가 완료되었습니다.");
      onActionComplete?.();
      router.refresh();
    } catch {
      toast.error("반납 처리 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCancel() {
    if (cancelReason.length < 5) {
      toast.error("취소 사유를 입력해주세요.");
      return;
    }
    setIsLoading(true);
    try {
      await cancelRentalApi(rental.rentalId, { reason: cancelReason });
      toast.success("대여를 취소했습니다.");
      setShowCancelModal(false);
      onActionComplete?.();
      router.refresh();
    } catch {
      toast.error("취소 처리 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  const canCancel =
    (rental.status === "REQUESTED" || rental.status === "APPROVED") &&
    (isRenter || isLender);

  return (
    <div data-testid="rental-action-buttons" className="flex flex-col gap-3">
      {/* 등록자: REQUESTED 상태에서 승인/거절 */}
      {isLender && rental.status === "REQUESTED" && (
        <div className="flex gap-3">
          <button
            data-testid="btn-reject"
            onClick={() => setShowRejectModal(true)}
            disabled={isLoading}
            className="flex-1 py-3 px-4 rounded-lg border border-red-300 text-red-600 font-medium hover:bg-red-50 disabled:opacity-50 transition-colors"
          >
            거절
          </button>
          <button
            data-testid="btn-approve"
            onClick={handleApprove}
            disabled={isLoading}
            className="flex-1 py-3 px-4 rounded-lg bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {isLoading ? "처리 중..." : "승인"}
          </button>
        </div>
      )}

      {/* 대여자: APPROVED 상태에서 결제하기 */}
      {isRenter && rental.status === "APPROVED" && (
        <button
          data-testid="btn-pay"
          onClick={() => router.push(`/rentals/${rental.rentalId}/payment`)}
          className="w-full py-3 px-4 rounded-lg bg-green-600 text-white font-medium hover:bg-green-700 transition-colors"
        >
          결제하기
        </button>
      )}

      {/* 등록자: PAID 상태에서 배송 시작 */}
      {isLender && rental.status === "PAID" && (
        <button
          data-testid="btn-start"
          onClick={handleStart}
          disabled={isLoading}
          className="w-full py-3 px-4 rounded-lg bg-orange-500 text-white font-medium hover:bg-orange-600 disabled:opacity-50 transition-colors"
        >
          {isLoading ? "처리 중..." : "배송 시작"}
        </button>
      )}

      {/* 대여자: IN_USE 상태에서 반납 처리 */}
      {isRenter && rental.status === "IN_USE" && (
        <button
          data-testid="btn-return"
          onClick={handleReturn}
          disabled={isLoading}
          className="w-full py-3 px-4 rounded-lg bg-slate-600 text-white font-medium hover:bg-slate-700 disabled:opacity-50 transition-colors"
        >
          {isLoading ? "처리 중..." : "반납 처리"}
        </button>
      )}

      {/* 취소 버튼 (REQUESTED/APPROVED 상태에서 대여자/등록자 모두 가능) */}
      {canCancel && (
        <button
          data-testid="btn-cancel"
          onClick={() => setShowCancelModal(true)}
          disabled={isLoading}
          className="w-full py-2 px-4 rounded-lg border border-gray-300 text-gray-600 text-sm hover:bg-gray-50 disabled:opacity-50 transition-colors"
        >
          취소하기
        </button>
      )}

      {/* 거절 모달 */}
      {showRejectModal && (
        <div
          data-testid="reject-modal"
          className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
        >
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              대여 요청 거절
            </h3>
            <p className="text-sm text-gray-500 mb-4">
              거절 사유를 입력해주세요. (최소 10자)
            </p>
            <textarea
              data-testid="reject-reason-input"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="거절 사유를 입력하세요..."
              className="w-full border border-gray-300 rounded-lg p-3 text-sm resize-none h-24 focus:outline-none focus:ring-2 focus:ring-red-300"
            />
            <div className="flex gap-3 mt-4">
              <button
                onClick={() => {
                  setShowRejectModal(false);
                  setRejectReason("");
                }}
                className="flex-1 py-2 px-4 rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50"
              >
                취소
              </button>
              <button
                data-testid="btn-confirm-reject"
                onClick={handleReject}
                disabled={isLoading || rejectReason.length < 10}
                className="flex-1 py-2 px-4 rounded-lg bg-red-500 text-white hover:bg-red-600 disabled:opacity-50"
              >
                {isLoading ? "처리 중..." : "거절 확인"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 취소 모달 */}
      {showCancelModal && (
        <div
          data-testid="cancel-modal"
          className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
        >
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              대여 취소
            </h3>
            <p className="text-sm text-gray-500 mb-4">취소 사유를 입력해주세요.</p>
            <textarea
              data-testid="cancel-reason-input"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder="취소 사유를 입력하세요..."
              className="w-full border border-gray-300 rounded-lg p-3 text-sm resize-none h-24 focus:outline-none focus:ring-2 focus:ring-gray-300"
            />
            <div className="flex gap-3 mt-4">
              <button
                onClick={() => {
                  setShowCancelModal(false);
                  setCancelReason("");
                }}
                className="flex-1 py-2 px-4 rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50"
              >
                닫기
              </button>
              <button
                data-testid="btn-confirm-cancel"
                onClick={handleCancel}
                disabled={isLoading || cancelReason.length < 5}
                className="flex-1 py-2 px-4 rounded-lg bg-gray-700 text-white hover:bg-gray-800 disabled:opacity-50"
              >
                {isLoading ? "처리 중..." : "취소 확인"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
