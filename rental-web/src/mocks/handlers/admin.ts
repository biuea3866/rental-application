import { http, HttpResponse, delay } from "msw";
import type {
  AdminDashboardResponse,
  AdminRentalResponse,
  AdminRentalListResponse,
  RentalStatus,
} from "@/lib/api/types";

// ========================================
// Admin MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ========================================
// 스텁 데이터
// ========================================

export const STUB_ADMIN_DASHBOARD: AdminDashboardResponse = {
  rentalCounts: {
    REQUESTED: 12,
    APPROVED: 8,
    PAID: 5,
    IN_USE: 23,
    RETURNED: 47,
    CANCELLED: 9,
  },
  dailyRevenue: [
    { date: "2026-04-13", amount: 280000 },
    { date: "2026-04-14", amount: 350000 },
    { date: "2026-04-15", amount: 420000 },
    { date: "2026-04-16", amount: 190000 },
    { date: "2026-04-17", amount: 510000 },
    { date: "2026-04-18", amount: 380000 },
    { date: "2026-04-19", amount: 290000 },
  ],
  weeklyRevenue: [
    { weekStart: "2026-03-24", amount: 1850000 },
    { weekStart: "2026-03-31", amount: 2100000 },
    { weekStart: "2026-04-07", amount: 2450000 },
    { weekStart: "2026-04-14", amount: 2420000 },
  ],
};

export const STUB_ADMIN_RENTALS: AdminRentalResponse[] = [
  {
    rentalId: 1001,
    renterId: 11,
    lenderId: 22,
    productId: 42,
    productName: "캠핑 텐트 A",
    status: "IN_USE",
    totalAmount: 120000,
    startDate: "2026-05-01",
    endDate: "2026-05-07",
    createdAt: "2026-04-14T10:00:00+09:00",
  },
  {
    rentalId: 1002,
    renterId: 11,
    lenderId: 33,
    productId: 43,
    productName: "소니 A7C II 카메라",
    status: "REQUESTED",
    totalAmount: 75000,
    startDate: "2026-05-10",
    endDate: "2026-05-15",
    createdAt: "2026-04-14T11:00:00+09:00",
  },
  {
    rentalId: 1003,
    renterId: 44,
    lenderId: 22,
    productId: 44,
    productName: "캠핑 의자 세트",
    status: "APPROVED",
    totalAmount: 30000,
    startDate: "2026-05-20",
    endDate: "2026-05-22",
    createdAt: "2026-04-13T09:00:00+09:00",
  },
  {
    rentalId: 1004,
    renterId: 55,
    lenderId: 22,
    productId: 45,
    productName: "드론 DJI Mini 4 Pro",
    status: "RETURNED",
    totalAmount: 200000,
    startDate: "2026-04-01",
    endDate: "2026-04-07",
    createdAt: "2026-03-28T10:00:00+09:00",
  },
  {
    rentalId: 1005,
    renterId: 66,
    lenderId: 33,
    productId: 46,
    productName: "전동 킥보드",
    status: "CANCELLED",
    totalAmount: 50000,
    startDate: "2026-04-15",
    endDate: "2026-04-17",
    createdAt: "2026-04-10T14:00:00+09:00",
  },
];

/** 정지된 유저 ID 세트 */
let suspendedUsers = new Set<number>();

export function resetAdminHandlerState(): void {
  suspendedUsers = new Set<number>();
}

// ========================================
// MSW 핸들러
// ========================================

export const adminHandlers = [
  // 관리자 대시보드 — GET /api/v1/admin/dashboard
  http.get(`${BASE_URL}/api/v1/admin/dashboard`, async () => {
    await delay(200);
    return HttpResponse.json(STUB_ADMIN_DASHBOARD);
  }),

  // 관리자 대여 목록 — GET /api/v1/admin/rentals
  http.get(`${BASE_URL}/api/v1/admin/rentals`, async ({ request }) => {
    await delay(200);
    const url = new URL(request.url);
    const statusFilter = url.searchParams.get("status") as RentalStatus | null;
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    let filtered = STUB_ADMIN_RENTALS;
    if (statusFilter) {
      filtered = filtered.filter((r) => r.status === statusFilter);
    }

    const totalElements = filtered.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const start = page * size;
    const content = filtered.slice(start, start + size);

    const response: AdminRentalListResponse = {
      content,
      totalElements,
      totalPages,
    };
    return HttpResponse.json(response);
  }),

  // 유저 정지 — POST /api/v1/admin/users/:userId/suspend
  http.post(
    `${BASE_URL}/api/v1/admin/users/:userId/suspend`,
    async ({ params }) => {
      await delay(200);
      const userId = Number(params.userId);
      suspendedUsers.add(userId);
      return HttpResponse.json({ success: true, userId });
    }
  ),

  // 유저 활성화 — POST /api/v1/admin/users/:userId/activate
  http.post(
    `${BASE_URL}/api/v1/admin/users/:userId/activate`,
    async ({ params }) => {
      await delay(200);
      const userId = Number(params.userId);
      suspendedUsers.delete(userId);
      return HttpResponse.json({ success: true, userId });
    }
  ),
];
