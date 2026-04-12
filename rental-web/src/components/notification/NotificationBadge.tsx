"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { useNotificationStore } from "@/stores/notification-store";
import type { UnreadCountResponse } from "@/lib/api/types";

// ========================================
// NotificationBadge 컴포넌트 (헤더용)
// ========================================

export function NotificationBadge() {
  const apiClient = getApiClient();
  const { unreadCount, setUnreadCount } = useNotificationStore();

  // 미읽음 수 주기적 조회 (30초마다 갱신)
  const { data } = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: async () => {
      const response = await apiClient.get<UnreadCountResponse>(
        ENDPOINTS.NOTIFICATIONS.UNREAD_COUNT
      );
      return response.data;
    },
    refetchInterval: 30_000,
  });

  useEffect(() => {
    if (data !== undefined) {
      setUnreadCount(data.count);
    }
  }, [data, setUnreadCount]);

  return (
    <Link
      href="/notifications"
      className="relative inline-flex items-center justify-center rounded-full p-2 hover:bg-muted"
      aria-label={`알림 ${unreadCount > 0 ? `(읽지 않은 알림 ${unreadCount}개)` : ""}`}
    >
      {/* 벨 아이콘 */}
      <svg
        xmlns="http://www.w3.org/2000/svg"
        className="h-5 w-5"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>

      {/* 미읽음 배지 */}
      {unreadCount > 0 && (
        <span
          className="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-destructive-foreground"
          aria-hidden="true"
        >
          {unreadCount > 99 ? "99+" : unreadCount}
        </span>
      )}
    </Link>
  );
}
