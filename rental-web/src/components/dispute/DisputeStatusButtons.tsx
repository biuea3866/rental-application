"use client";

import { useState } from "react";
import { toast } from "sonner";
import { cancelDisputeApi } from "@/lib/api/dispute";
import type { DisputeResult } from "@/lib/api/types";

// ========================================
// DisputeStatusButtons — 분쟁 상태별 버튼 (FE-450)
// OPEN 상태에서만 '취소' 버튼 표시
// ========================================

interface DisputeStatusButtonsProps {
  dispute: DisputeResult;
  onCancelled: () => void;
}

export function DisputeStatusButtons({
  dispute,
  onCancelled,
}: DisputeStatusButtonsProps) {
  const [isLoading, setIsLoading] = useState(false);

  async function handleCancel() {
    setIsLoading(true);
    try {
      await cancelDisputeApi(dispute.id);
      toast.success("분쟁이 취소되었습니다.");
      onCancelled();
    } catch (err) {
      const message = err instanceof Error ? err.message : "분쟁 취소에 실패했습니다.";
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  }

  if (dispute.status !== "OPEN") return null;

  return (
    <div className="flex gap-3">
      <button
        onClick={handleCancel}
        disabled={isLoading}
        className="w-full py-2.5 px-4 rounded-lg border border-red-300 text-red-600 text-sm font-medium hover:bg-red-50 disabled:opacity-50 transition-colors"
      >
        {isLoading ? "처리 중..." : "분쟁 취소"}
      </button>
    </div>
  );
}
