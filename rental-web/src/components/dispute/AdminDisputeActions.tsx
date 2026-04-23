"use client";

import { useState } from "react";
import { toast } from "sonner";
import { adminReviewDisputeApi } from "@/lib/api/dispute";
import { AdminDisputeResolveModal } from "./AdminDisputeResolveModal";
import type { DisputeResult } from "@/lib/api/types";

// ========================================
// AdminDisputeActions — 관리자 분쟁 액션 버튼 (FE-451)
// OPEN → 검토 시작 / UNDER_REVIEW → 해결 / 해결됨 → PG pending 뱃지
// ========================================

interface AdminDisputeActionsProps {
  dispute: DisputeResult;
  onUpdated: () => void;
}

const PG_PENDING_STATUSES = ["RESOLVED_REFUND", "RESOLVED_PARTIAL"] as const;

export function AdminDisputeActions({
  dispute,
  onUpdated,
}: AdminDisputeActionsProps) {
  const [isReviewing, setIsReviewing] = useState(false);
  const [showResolveModal, setShowResolveModal] = useState(false);

  async function handleStartReview() {
    setIsReviewing(true);
    try {
      await adminReviewDisputeApi(dispute.id);
      toast.success("검토가 시작되었습니다.");
      onUpdated();
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "검토 시작에 실패했습니다.";
      toast.error(message);
    } finally {
      setIsReviewing(false);
    }
  }

  const isPgPending = (PG_PENDING_STATUSES as readonly string[]).includes(
    dispute.status
  );

  return (
    <div className="space-y-3">
      {/* PG pending 뱃지 */}
      {isPgPending && (
        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-orange-100 text-orange-700 text-xs font-medium">
          <span className="w-1.5 h-1.5 rounded-full bg-orange-500 animate-pulse" />
          PG pending
        </div>
      )}

      {/* OPEN: 검토 시작 버튼 */}
      {dispute.status === "OPEN" && (
        <button
          onClick={handleStartReview}
          disabled={isReviewing}
          className="w-full py-2.5 px-4 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
        >
          {isReviewing ? "처리 중..." : "검토 시작"}
        </button>
      )}

      {/* UNDER_REVIEW: 해결 버튼 */}
      {dispute.status === "UNDER_REVIEW" && (
        <button
          onClick={() => setShowResolveModal(true)}
          className="w-full py-2.5 px-4 rounded-lg bg-green-600 text-white text-sm font-medium hover:bg-green-700 transition-colors"
        >
          해결
        </button>
      )}

      {/* 해결 모달 */}
      <AdminDisputeResolveModal
        dispute={dispute}
        isOpen={showResolveModal}
        onClose={() => setShowResolveModal(false)}
        onResolved={() => {
          setShowResolveModal(false);
          onUpdated();
        }}
      />
    </div>
  );
}
