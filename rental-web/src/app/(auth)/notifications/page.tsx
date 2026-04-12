"use client";

import { NotificationList } from "@/components/notification/NotificationList";

// ========================================
// 알림 목록 페이지
// ========================================

export default function NotificationsPage() {
  return (
    <main className="flex flex-1 flex-col gap-6 p-6">
      <h1 className="text-2xl font-bold">알림</h1>
      <NotificationList />
    </main>
  );
}
