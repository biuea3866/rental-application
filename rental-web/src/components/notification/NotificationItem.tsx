"use client";

import { forwardRef } from "react";
import type { Notification } from "@/lib/api/types";

// ========================================
// 알림 타입별 한국어 레이블
// ========================================

const NOTIFICATION_TYPE_LABELS: Record<Notification["type"], string> = {
  RENTAL_REQUEST: "대여 요청",
  RENTAL_APPROVED: "승인",
  RENTAL_REJECTED: "거절",
  RENTAL_RETURNED: "반납",
  PAYMENT_COMPLETED: "결제",
  SYSTEM: "시스템",
};

// ========================================
// NotificationItem 컴포넌트
// ========================================

interface NotificationItemProps {
  notification: Notification;
  onRead?: (id: string) => void;
}

export const NotificationItem = forwardRef<
  HTMLLIElement,
  NotificationItemProps
>(function NotificationItem({ notification, onRead }, ref) {
  const handleClick = () => {
    if (!notification.isRead && onRead) {
      onRead(notification.id);
    }
  };

  return (
    <li
      ref={ref}
      role="listitem"
      className={`flex cursor-pointer gap-3 rounded-lg border p-4 transition-colors hover:bg-muted/50 ${
        notification.isRead
          ? "border-transparent bg-transparent"
          : "border-primary/20 bg-primary/5"
      }`}
      onClick={handleClick}
      aria-label={notification.isRead ? "읽은 알림" : "읽지 않은 알림"}
    >
      {/* 읽음 인디케이터 */}
      <div className="mt-1 flex-shrink-0">
        <span
          className={`block h-2 w-2 rounded-full ${
            notification.isRead ? "bg-transparent" : "bg-primary"
          }`}
          aria-hidden="true"
        />
      </div>

      {/* 알림 내용 */}
      <div className="flex-1 space-y-1">
        <div className="flex items-center gap-2">
          <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
            {NOTIFICATION_TYPE_LABELS[notification.type] ?? notification.type}
          </span>
          <span className="text-xs text-muted-foreground">
            {new Date(notification.createdAt).toLocaleDateString("ko-KR", {
              month: "long",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            })}
          </span>
        </div>
        <p className="text-sm font-medium">{notification.title}</p>
        <p className="text-sm text-muted-foreground">{notification.message}</p>
      </div>
    </li>
  );
});
