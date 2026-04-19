"use client";

import { useState } from "react";
import { suspendUserApi, activateUserApi } from "@/lib/api/admin";

// ========================================
// 유저 관리 행 컴포넌트
// ========================================

interface UserManagementRowProps {
  userId: number;
  name: string;
  email: string;
  isSuspended: boolean;
  onStatusChange?: (userId: number, isSuspended: boolean) => void;
}

export function UserManagementRow({
  userId,
  name,
  email,
  isSuspended,
  onStatusChange,
}: UserManagementRowProps) {
  const [suspended, setSuspended] = useState(isSuspended);
  const [isLoading, setIsLoading] = useState(false);

  const handleToggle = async () => {
    setIsLoading(true);
    try {
      if (suspended) {
        await activateUserApi(userId);
        setSuspended(false);
        onStatusChange?.(userId, false);
      } else {
        await suspendUserApi(userId);
        setSuspended(true);
        onStatusChange?.(userId, true);
      }
    } catch {
      // 에러 처리 (toast 등)
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      data-testid={`user-management-row-${userId}`}
      className="flex items-center justify-between px-4 py-3 border-b border-gray-100 last:border-0 hover:bg-gray-50"
    >
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 truncate">{name}</p>
        <p className="text-xs text-gray-500 truncate">{email}</p>
        <p className="text-xs text-gray-400">ID #{userId}</p>
      </div>

      <div className="flex items-center gap-3 ml-4">
        {/* 상태 배지 */}
        <span
          data-testid={`user-status-${userId}`}
          className={[
            "text-xs font-medium px-2 py-0.5 rounded-full",
            suspended
              ? "bg-red-100 text-red-700"
              : "bg-green-100 text-green-700",
          ].join(" ")}
        >
          {suspended ? "정지됨" : "정상"}
        </span>

        {/* 토글 버튼 */}
        <button
          data-testid={`user-toggle-btn-${userId}`}
          onClick={handleToggle}
          disabled={isLoading}
          className={[
            "px-3 py-1.5 rounded-lg text-xs font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed",
            suspended
              ? "bg-green-600 text-white hover:bg-green-700"
              : "bg-red-50 text-red-600 border border-red-200 hover:bg-red-100",
          ].join(" ")}
        >
          {isLoading ? "처리 중..." : suspended ? "활성화" : "정지"}
        </button>
      </div>
    </div>
  );
}
