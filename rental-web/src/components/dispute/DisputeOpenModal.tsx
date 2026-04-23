"use client";

import { useState, useId, useRef, useEffect } from "react";
import { toast } from "sonner";
import { openDisputeApi } from "@/lib/api/dispute";
import { ApiRequestError } from "@/lib/api/client";
import type { DisputeReason } from "@/lib/api/types";

// ========================================
// DisputeOpenModal — 분쟁 오픈 모달 (FE-450)
// 사유 드롭다운 + textarea + 이미지 최대 5장
// ========================================

const DISPUTE_REASON_LABELS: Record<DisputeReason, string> = {
  DAMAGED: "파손",
  NOT_RETURNED: "미반납",
  LATE_RETURN: "늦은 반납",
  WRONG_ITEM: "잘못된 상품",
  OTHER: "기타",
};

interface DisputeOpenModalProps {
  rentalId: number;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function DisputeOpenModal({
  rentalId,
  isOpen,
  onClose,
  onSuccess,
}: DisputeOpenModalProps) {
  const titleId = useId();
  const [reason, setReason] = useState<DisputeReason>("DAMAGED");
  const [description, setDescription] = useState("");
  const [attachmentUrls, setAttachmentUrls] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const firstFocusRef = useRef<HTMLSelectElement>(null);

  const descLength = description.length;
  const isDescValid = descLength >= 1 && descLength <= 1000;
  const isSubmitDisabled = !isDescValid || isSubmitting;

  // focus trap: 모달 열릴 때 첫 요소 포커스
  useEffect(() => {
    if (isOpen) {
      firstFocusRef.current?.focus();
    }
  }, [isOpen]);

  // 닫힐 때 상태 초기화
  function handleClose() {
    setReason("DAMAGED");
    setDescription("");
    setAttachmentUrls([]);
    setErrorMsg(null);
    onClose();
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (isSubmitDisabled) return;

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      await openDisputeApi({ rentalId, reason, description, attachmentUrls });
      toast.success("분쟁이 오픈되었습니다.");
      handleClose();
      onSuccess();
    } catch (err) {
      if (err instanceof ApiRequestError && err.status === 409) {
        setErrorMsg("이미 진행 중인 분쟁이 있습니다.");
        toast.error("이미 진행 중인 분쟁이 있습니다.");
      } else {
        const message = err instanceof Error ? err.message : "분쟁 오픈에 실패했습니다.";
        setErrorMsg(message);
        toast.error(message);
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  // 이미지 첨부 (presigned URL 대신 URL 직접 입력 — 실제 구현에선 presigned 훅 사용)
  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length + attachmentUrls.length > 5) {
      toast.error("이미지는 최대 5장까지 첨부할 수 있습니다.");
      return;
    }
    // stub: 파일명을 URL로 사용 (실제 구현에선 presigned URL 업로드 후 key 저장)
    const newUrls = files.map((f) => URL.createObjectURL(f));
    setAttachmentUrls((prev) => [...prev, ...newUrls].slice(0, 5));
  }

  function removeAttachment(index: number) {
    setAttachmentUrls((prev) => prev.filter((_, i) => i !== index));
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
        className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl max-h-[90vh] overflow-y-auto"
      >
        <h2
          id={titleId}
          className="text-lg font-semibold text-gray-900 mb-4"
        >
          분쟁 오픈
        </h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* 분쟁 사유 */}
          <div>
            <label
              htmlFor="dispute-reason"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              분쟁 사유
            </label>
            <select
              id="dispute-reason"
              ref={firstFocusRef}
              value={reason}
              onChange={(e) => setReason(e.target.value as DisputeReason)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
            >
              {(Object.keys(DISPUTE_REASON_LABELS) as DisputeReason[]).map((key) => (
                <option key={key} value={key}>
                  {DISPUTE_REASON_LABELS[key]}
                </option>
              ))}
            </select>
          </div>

          {/* 상세 설명 */}
          <div>
            <label
              htmlFor="dispute-description"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              상세 설명
            </label>
            <textarea
              id="dispute-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="분쟁 내용을 자세히 설명해주세요. (1~1000자)"
              maxLength={1001}
              rows={4}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-300"
            />
            <div className="flex justify-between text-xs mt-1">
              {descLength > 1000 ? (
                <span className="text-red-500">1000자 이내로 입력해주세요.</span>
              ) : (
                <span className="text-gray-400" />
              )}
              <span className={descLength > 1000 ? "text-red-500" : "text-gray-400"}>
                {descLength} / 1000
              </span>
            </div>
          </div>

          {/* 이미지 첨부 */}
          <div>
            <p className="text-sm font-medium text-gray-700 mb-1">
              증빙 이미지 첨부 <span className="text-gray-400 font-normal">(최대 5장)</span>
            </p>
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={handleFileChange}
              className="block w-full text-sm text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
              aria-label="증빙 이미지 첨부 (최대 5장)"
            />
            {attachmentUrls.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {attachmentUrls.map((url, idx) => (
                  <div key={idx} className="relative w-16 h-16">
                    <img
                      src={url}
                      alt={`첨부 이미지 ${idx + 1}`}
                      className="w-full h-full object-cover rounded-lg border border-gray-200"
                    />
                    <button
                      type="button"
                      onClick={() => removeAttachment(idx)}
                      className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-red-500 text-white text-xs flex items-center justify-center"
                      aria-label={`이미지 ${idx + 1} 제거`}
                    >
                      x
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 에러 메시지 */}
          {errorMsg && (
            <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
              {errorMsg}
            </p>
          )}

          {/* 버튼 */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 py-2.5 px-4 rounded-lg border border-gray-300 text-gray-600 text-sm hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSubmitDisabled}
              className="flex-1 py-2.5 px-4 rounded-lg bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isSubmitting ? "처리 중..." : "분쟁 오픈"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
