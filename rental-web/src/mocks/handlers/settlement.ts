import { http, HttpResponse, delay } from "msw";
import type { SettlementResponse } from "@/lib/api/types";

// ========================================
// Settlement MSW 핸들러
// RC-FE-320
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ========================================
// 스텁 데이터
// ========================================

export const STUB_SETTLEMENTS: SettlementResponse[] = [
  {
    settlementId: 1,
    lenderId: 22,
    rentalId: 1001,
    amount: 120000,
    commissionRate: 0.1,
    commissionAmount: 12000,
    netAmount: 108000,
    status: "COMPLETED",
    createdAt: "2026-04-12T10:00:00+09:00",
  },
  {
    settlementId: 2,
    lenderId: 22,
    rentalId: 1002,
    amount: 75000,
    commissionRate: 0.1,
    commissionAmount: 7500,
    netAmount: 67500,
    status: "PENDING",
    createdAt: "2026-04-14T11:00:00+09:00",
  },
  {
    settlementId: 3,
    lenderId: 22,
    rentalId: 1003,
    amount: 30000,
    commissionRate: 0.1,
    commissionAmount: 3000,
    netAmount: 27000,
    status: "COMPLETED",
    createdAt: "2026-04-09T09:00:00+09:00",
  },
  {
    settlementId: 4,
    lenderId: 22,
    rentalId: 1004,
    amount: 50000,
    commissionRate: 0.1,
    commissionAmount: 5000,
    netAmount: 45000,
    status: "PENDING",
    createdAt: "2026-04-15T14:00:00+09:00",
  },
];

let stubSettlements: SettlementResponse[] = [...STUB_SETTLEMENTS];

export function resetSettlementHandlerState(): void {
  stubSettlements = [...STUB_SETTLEMENTS];
}

// ========================================
// MSW 핸들러
// ========================================

export const settlementHandlers = [
  // 내 정산 목록 — GET /api/v1/my/settlements
  http.get(`${BASE_URL}/api/v1/my/settlements`, async ({ request }) => {
    await delay(200);
    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "10", 10);

    const totalElements = stubSettlements.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const start = page * size;
    const content = stubSettlements.slice(start, start + size);

    return HttpResponse.json({
      success: true,
      data: { content, totalElements, totalPages },
      timestamp: new Date().toISOString(),
    });
  }),
];
