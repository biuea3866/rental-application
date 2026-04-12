import { http, HttpResponse, delay } from "msw";
import type { Notification } from "@/lib/api/types";

// ========================================
// Notification MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 스텁 알림 데이터
export const STUB_NOTIFICATIONS: Notification[] = [
  {
    id: "notif-001",
    type: "RENTAL_REQUEST",
    title: "대여 요청이 들어왔습니다",
    message: "캠핑 텐트에 대한 대여 요청이 접수되었습니다.",
    isRead: false,
    createdAt: "2026-04-12T09:00:00Z",
    relatedId: "rental-001",
  },
  {
    id: "notif-002",
    type: "RENTAL_APPROVED",
    title: "대여 요청이 승인되었습니다",
    message: "DSLR 카메라 대여 요청이 승인되었습니다.",
    isRead: false,
    createdAt: "2026-04-11T15:30:00Z",
    relatedId: "rental-002",
  },
  {
    id: "notif-003",
    type: "PAYMENT_COMPLETED",
    title: "결제가 완료되었습니다",
    message: "대여료 결제가 성공적으로 처리되었습니다.",
    isRead: true,
    createdAt: "2026-04-10T12:00:00Z",
    relatedId: "rental-003",
  },
  {
    id: "notif-004",
    type: "RENTAL_RETURNED",
    title: "물건이 반납되었습니다",
    message: "전동 킥보드가 정상적으로 반납되었습니다.",
    isRead: true,
    createdAt: "2026-04-09T18:00:00Z",
    relatedId: "rental-004",
  },
  {
    id: "notif-005",
    type: "SYSTEM",
    title: "서비스 점검 안내",
    message: "4월 15일 오전 2시~4시 서비스 점검이 예정되어 있습니다.",
    isRead: true,
    createdAt: "2026-04-08T10:00:00Z",
  },
];

let notifications: Notification[] = STUB_NOTIFICATIONS.map((n) => ({ ...n }));

export function resetNotificationHandlerState(): void {
  notifications = STUB_NOTIFICATIONS.map((n) => ({ ...n }));
}

export const notificationHandlers = [
  // 알림 목록 (페이징)
  http.get(`${BASE_URL}/api/v1/notifications`, async ({ request }) => {
    await delay(150);

    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get("page") || "0", 10);
    const size = parseInt(url.searchParams.get("size") || "10", 10);

    const start = page * size;
    const end = start + size;
    const pageContent = notifications.slice(start, end);

    return HttpResponse.json({
      success: true,
      data: {
        content: pageContent,
        page,
        size,
        totalElements: notifications.length,
        totalPages: Math.ceil(notifications.length / size),
        hasNext: end < notifications.length,
      },
      timestamp: new Date().toISOString(),
    });
  }),

  // 읽음 처리
  http.patch(`${BASE_URL}/api/v1/notifications/:id/read`, async ({ params }) => {
    await delay(100);

    const id = params.id as string;
    const index = notifications.findIndex((n) => n.id === id);

    if (index === -1) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "알림을 찾을 수 없습니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 404 }
      );
    }

    notifications[index] = { ...notifications[index], isRead: true };

    return HttpResponse.json({
      success: true,
      data: notifications[index],
      timestamp: new Date().toISOString(),
    });
  }),

  // 전체 읽음 처리
  http.patch(`${BASE_URL}/api/v1/notifications/read-all`, async () => {
    await delay(150);

    notifications = notifications.map((n) => ({ ...n, isRead: true }));

    return HttpResponse.json({
      success: true,
      data: null,
      timestamp: new Date().toISOString(),
    });
  }),

  // 미읽음 수 조회
  http.get(`${BASE_URL}/api/v1/notifications/unread-count`, async () => {
    await delay(100);

    const count = notifications.filter((n) => !n.isRead).length;

    return HttpResponse.json({
      success: true,
      data: { count },
      timestamp: new Date().toISOString(),
    });
  }),
];
