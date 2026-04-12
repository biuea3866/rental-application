"use client";

import { useCallback, useRef } from "react";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { NotificationItem } from "./NotificationItem";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { useNotificationStore } from "@/stores/notification-store";
import type { Notification, PaginatedResponse } from "@/lib/api/types";

// ========================================
// NotificationList 컴포넌트
// ========================================

const PAGE_SIZE = 10;

export function NotificationList() {
  const apiClient = getApiClient();
  const queryClient = useQueryClient();
  const { decrementUnreadCount, clearUnreadCount } = useNotificationStore();
  const observerRef = useRef<IntersectionObserver | null>(null);

  // 알림 목록 무한스크롤 쿼리
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
  } = useInfiniteQuery({
    queryKey: ["notifications"],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await apiClient.get<PaginatedResponse<Notification>>(
        `${ENDPOINTS.NOTIFICATIONS.BASE}?page=${pageParam}&size=${PAGE_SIZE}`
      );
      return response.data;
    },
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.page + 1 : undefined,
    initialPageParam: 0,
  });

  // 읽음 처리
  const readMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.patch(ENDPOINTS.NOTIFICATIONS.READ(id));
    },
    onSuccess: (_, id) => {
      queryClient.setQueryData(
        ["notifications"],
        (old: { pages: PaginatedResponse<Notification>[] } | undefined) => {
          if (!old) return old;
          return {
            ...old,
            pages: old.pages.map((page) => ({
              ...page,
              content: page.content.map((n) =>
                n.id === id ? { ...n, isRead: true } : n
              ),
            })),
          };
        }
      );
      decrementUnreadCount();
    },
  });

  // 전체 읽음 처리
  const readAllMutation = useMutation({
    mutationFn: async () => {
      await apiClient.patch(ENDPOINTS.NOTIFICATIONS.READ_ALL);
    },
    onSuccess: () => {
      queryClient.setQueryData(
        ["notifications"],
        (old: { pages: PaginatedResponse<Notification>[] } | undefined) => {
          if (!old) return old;
          return {
            ...old,
            pages: old.pages.map((page) => ({
              ...page,
              content: page.content.map((n) => ({ ...n, isRead: true })),
            })),
          };
        }
      );
      clearUnreadCount();
    },
  });

  // 무한스크롤 감지 콜백
  const lastItemRef = useCallback(
    (node: HTMLLIElement | null) => {
      if (isFetchingNextPage) return;
      if (observerRef.current) observerRef.current.disconnect();
      observerRef.current = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && hasNextPage) {
          fetchNextPage();
        }
      });
      if (node) observerRef.current.observe(node);
    },
    [isFetchingNextPage, hasNextPage, fetchNextPage]
  );

  const allNotifications =
    (data?.pages.flatMap((page) => page.content) ?? []).filter(Boolean);

  const unreadCount = allNotifications.filter((n) => !n.isRead).length;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-sm text-muted-foreground">알림을 불러오는 중...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-sm text-destructive">알림을 불러오지 못했습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {unreadCount > 0 ? `읽지 않은 알림 ${unreadCount}개` : "모두 읽었습니다"}
        </p>
        {unreadCount > 0 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => readAllMutation.mutate()}
            disabled={readAllMutation.isPending}
          >
            전체 읽음
          </Button>
        )}
      </div>

      {/* 알림 목록 */}
      {allNotifications.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <p className="text-muted-foreground">알림이 없습니다.</p>
        </div>
      ) : (
        <ul className="space-y-2" role="list" aria-label="알림 목록">
          {allNotifications.map((notification, index) => {
            const isLast = index === allNotifications.length - 1;
            return (
              <NotificationItem
                key={notification.id}
                notification={notification}
                onRead={(id) => readMutation.mutate(id)}
                ref={isLast ? lastItemRef : undefined}
              />
            );
          })}
        </ul>
      )}

      {/* 로딩 인디케이터 */}
      {isFetchingNextPage && (
        <div className="flex justify-center py-4">
          <p className="text-sm text-muted-foreground">불러오는 중...</p>
        </div>
      )}
    </div>
  );
}
