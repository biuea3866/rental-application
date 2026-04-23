"use client";

import { useState, useId, useRef, useEffect } from "react";
import { toast } from "sonner";
import { adminResolveDisputeApi } from "@/lib/api/dispute";
import { ApiRequestError } from "@/lib/api/client";
import type { DisputeResult, DisputeResolveType } from "@/lib/api/types";

// ========================================
// AdminDisputeResolveModal — 관리자 분쟁 해결 모달 (FE-451)
// 전체환불 / 부분환불(금액 입력) / 기각
// ========================================

interface AdminDisputeResolveModalProps {
  dispute: DisputeResult;
  isOpen: boolean;
  onClose: () => void;
  onResolved: () => void;
}

export function AdminDisputeResolveModal({
  dispute,
  isOpen,
  onClose,
  onResolved,
}: AdminDisputeResolveModalProps) {
  const titleId = useId();
  const [selectedType, setSelectedType] = useState<DisputeResolveType | null>(
    null
  );
  const [refundAmount, setRefundAmount] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const firstBtnRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (isOpen) {
      firstBtnRef.current?.focus();
    }
  }, [isOpen]);

  function handleClose() {
    setSelectedType(null);
    setRefundAmount("");
    setErrorMsg(null);
    onClose();
  }

  const isConfirmDisabled =
    !selectedType ||
    (selectedType === "PARTIAL" && !refundAmount) ||
    isSubmitting;

  async function handleConfirm() {
    if (!selectedType || isConfirmDisabled) return;

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      await adminResolveDisputeApi(dispute.id, {
        type: selectedType,
        refundAmount:
          selectedType === "PARTIAL" ? Number(refundAmount) : undefined,
      });
      toast.success("분쟁이 해결되었습니다.");
      handleClose();
      onResolved();
    } catch (err) {
      if (err instanceof ApiRequestError) {
        const msg = err.apiError.message ?? "분쟁 해결에 실패했습니다.";
        setErrorMsg(msg);
        toast.error(msg);
      } else {
        const msg =
          err instanceof Error ? err.message : "분쟁 해결에 실패했습니다.";
        setErrorMsg(msg);
        toast.error(msg);
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) handleClose();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl"
      >
        <h2 id={titleId} className="text-lg font-semibold text-gray-900 mb-1">
          분쟁 해결
        </h2>
        <p className="text-sm text-gray-500 mb-4">
          분쟁 #{dispute.id} — {dispute.reason}
        </p>

        {/* 해결 유형 선택 */}
        <div className="flex gap-2 mb-4">
          <button
            ref={firstBtnRef}
            type="button"
            onClick={() => {
              setSelectedType("FULL_REFUND");
              setRefundAmount("");
            }}
            className={`flex-1 py-2 px-3 rounded-lg text-sm font-medium border transition-colors ${
              selectedType === "FULL_REFUND"
                ? "bg-green-600 text-white border-green-600"
                : "border-gray-300 text-gray-700 hover:bg-gray-50"
            }`}
          >
            전체 환불
          </button>
          <button
            type="button"
            onClick={() => setSelectedType("PARTIAL")}
            className={`flex-1 py-2 px-3 rounded-lg text-sm font-medium border transition-colors ${
              selectedType === "PARTIAL"
                ? "bg-yellow-500 text-white border-yellow-500"
                : "border-gray-300 text-gray-700 hover:bg-gray-50"
            }`}
          >
            부분 환불
          </button>
          <button
            type="button"
            onClick={() => {
              setSelectedType("REJECTED");
              setRefundAmount("");
            }}
            className={`flex-1 py-2 px-3 rounded-lg text-sm font-medium border transition-colors ${
              selectedType === "REJECTED"
                ? "bg-gray-700 text-white border-gray-700"
                : "border-gray-300 text-gray-700 hover:bg-gray-50"
            }`}
          >
            기각
          </button>
        </div>

        {/* 부분 환불 금액 입력 */}
        {selectedType === "PARTIAL" && (
          <div className="mb-4">
            <label
              htmlFor="refund-amount"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              환불 금액 (원)
            </label>
            <input
              id="refund-amount"
              type="number"
              min={0}
              value={refundAmount}
              onChange={(e) => setRefundAmount(e.target.value)}
              placeholder="환불할 금액을 입력하세요"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-300"
            />
          </div>
        )}

        {/* 에러 */}
        {errorMsg && (
          <div
            role="alert"
            className="mb-4 text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2"
          >
            {errorMsg}
          </div>
        )}

        {/* 버튼 */}
        <div className="flex gap-3">
          <button
            type="button"
            onClick={handleClose}
            className="flex-1 py-2.5 px-4 rounded-lg border border-gray-300 text-gray-600 text-sm hover:bg-gray-50 transition-colors"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={isConfirmDisabled}
            className="flex-1 py-2.5 px-4 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? "처리 중..." : "확인"}
          </button>
        </div>
      </div>
    </div>
  );
}
