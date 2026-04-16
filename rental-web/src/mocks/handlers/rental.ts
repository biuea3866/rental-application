import { http, HttpResponse, delay } from "msw";
import type {
  RentalSummary,
  RentalDetail,
  RentalCreatedResponse,
  RentalApproveResponse,
  RentalCancelResponse,
  ProcessPaymentResponse,
  RentalStartResponse,
  RentalReturnResponse,
  CreateRentalRequest,
  ProcessPaymentRequest,
  RentalRejectRequest,
} from "@/lib/api/types";

// ========================================
// Rental MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ========================================
// 스텁 데이터
// ========================================

export const STUB_RENTAL_SUMMARIES: RentalSummary[] = [
  {
    rentalId: 1001,
    productId: 42,
    productName: "캠핑 텐트 A",
    productThumbnailUrl: null,
    status: "IN_USE",
    startDate: "2026-05-01",
    endDate: "2026-05-07",
    totalAmount: 120000,
    requestedAt: "2026-04-14T10:00:00+09:00",
  },
  {
    rentalId: 1002,
    productId: 43,
    productName: "소니 A7C II 미러리스 카메라",
    productThumbnailUrl: null,
    status: "REQUESTED",
    startDate: "2026-05-10",
    endDate: "2026-05-15",
    totalAmount: 75000,
    requestedAt: "2026-04-14T11:00:00+09:00",
  },
  {
    rentalId: 1003,
    productId: 44,
    productName: "캠핑 의자 세트",
    productThumbnailUrl: null,
    status: "APPROVED",
    startDate: "2026-05-20",
    endDate: "2026-05-22",
    totalAmount: 30000,
    requestedAt: "2026-04-13T09:00:00+09:00",
  },
];

export const STUB_RENTAL_DETAIL: RentalDetail = {
  rentalId: 1001,
  product: {
    productId: 42,
    name: "캠핑 텐트 A",
    thumbnailUrl: null,
    category: "CAMPING",
  },
  renter: { userId: 11, name: "홍길동" },
  lender: { userId: 22, name: "김철수" },
  status: "IN_USE",
  startDate: "2026-05-01",
  endDate: "2026-05-07",
  totalAmount: 120000,
  depositAmount: 50000,
  deliveryInfo: {
    recipientName: "홍길동",
    recipientPhone: "010-1234-5678",
    addressLine1: "서울특별시 강남구 테헤란로 123",
    addressLine2: "101호",
    zipCode: "06234",
  },
  payment: {
    paymentId: 5001,
    amount: 120000,
    paymentMethod: "CARD",
    status: "COMPLETED",
    paidAt: "2026-04-14T12:00:00+09:00",
  },
  timeline: [
    { status: "REQUESTED", occurredAt: "2026-04-14T10:00:00+09:00" },
    { status: "APPROVED", occurredAt: "2026-04-14T11:00:00+09:00" },
    { status: "PAID", occurredAt: "2026-04-14T12:00:00+09:00" },
    { status: "IN_USE", occurredAt: "2026-04-15T09:00:00+09:00" },
  ],
  requestedAt: "2026-04-14T10:00:00+09:00",
  approvedAt: "2026-04-14T11:00:00+09:00",
  paidAt: "2026-04-14T12:00:00+09:00",
  startedAt: "2026-04-15T09:00:00+09:00",
};

// APPROVED 상태 스텁 (결제 페이지 테스트용)
export const STUB_RENTAL_APPROVED: RentalDetail = {
  rentalId: 1003,
  product: {
    productId: 44,
    name: "캠핑 의자 세트",
    thumbnailUrl: null,
    category: "CAMPING",
  },
  renter: { userId: 11, name: "홍길동" },
  lender: { userId: 22, name: "김철수" },
  status: "APPROVED",
  startDate: "2026-05-20",
  endDate: "2026-05-22",
  totalAmount: 30000,
  depositAmount: 10000,
  deliveryInfo: {
    recipientName: "홍길동",
    recipientPhone: "010-1234-5678",
    addressLine1: "서울특별시 강남구 테헤란로 123",
    addressLine2: "101호",
    zipCode: "06234",
  },
  timeline: [
    { status: "REQUESTED", occurredAt: "2026-04-13T09:00:00+09:00" },
    { status: "APPROVED", occurredAt: "2026-04-13T10:00:00+09:00" },
  ],
  requestedAt: "2026-04-13T09:00:00+09:00",
  approvedAt: "2026-04-13T10:00:00+09:00",
};

/** 핸들러 간 공유 상태 (테스트 격리용) */
let stubRentals: RentalDetail[] = [STUB_RENTAL_DETAIL, STUB_RENTAL_APPROVED];

export function resetRentalHandlerState(): void {
  stubRentals = [STUB_RENTAL_DETAIL, STUB_RENTAL_APPROVED];
}

// ========================================
// MSW 핸들러
// ========================================

export const rentalHandlers = [
  // 대여 목록 조회 — GET /api/v1/rentals
  http.get(`${BASE_URL}/api/v1/rentals`, async ({ request }) => {
    await delay(200);
    const url = new URL(request.url);
    const statusFilter = url.searchParams.get("status");
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    let summaries = STUB_RENTAL_SUMMARIES;
    if (statusFilter) {
      summaries = summaries.filter((r) => r.status === statusFilter);
    }

    const totalElements = summaries.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const start = page * size;
    const content = summaries.slice(start, start + size);
    const last = page >= totalPages - 1;

    return HttpResponse.json({
      content,
      number: page,
      size,
      totalElements,
      totalPages,
      last,
    });
  }),

  // 대여 상세 조회 — GET /api/v1/rentals/:rentalId
  http.get(`${BASE_URL}/api/v1/rentals/:rentalId`, async ({ params }) => {
    await delay(150);
    const rentalId = Number(params.rentalId);
    const rental = stubRentals.find((r) => r.rentalId === rentalId);
    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
        { status: 404 }
      );
    }
    return HttpResponse.json(rental);
  }),

  // 대여 신청 — POST /api/v1/rentals
  http.post(`${BASE_URL}/api/v1/rentals`, async ({ request }) => {
    await delay(300);
    const body = (await request.json()) as CreateRentalRequest;
    const newRentalId = 2000 + Math.floor(Math.random() * 1000);
    const result: RentalCreatedResponse = {
      rentalId: newRentalId,
      productId: body.productId,
      status: "REQUESTED",
      startDate: body.startDate,
      endDate: body.endDate,
      totalAmount: 70000,
      depositAmount: 50000,
      requestedAt: new Date().toISOString(),
    };
    return HttpResponse.json(result, { status: 201 });
  }),

  // 대여 승인 — PATCH /api/v1/rentals/:rentalId/approve
  http.patch(
    `${BASE_URL}/api/v1/rentals/:rentalId/approve`,
    async ({ params }) => {
      await delay(200);
      const rentalId = Number(params.rentalId);
      const rental = stubRentals.find((r) => r.rentalId === rentalId);
      if (!rental) {
        return HttpResponse.json(
          { code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
          { status: 404 }
        );
      }
      const result: RentalApproveResponse = {
        rentalId,
        status: "APPROVED",
        approvedAt: new Date().toISOString(),
      };
      return HttpResponse.json(result);
    }
  ),

  // 대여 거절 — PATCH /api/v1/rentals/:rentalId/reject
  http.patch(
    `${BASE_URL}/api/v1/rentals/:rentalId/reject`,
    async ({ params }) => {
      await delay(200);
      const rentalId = Number(params.rentalId);
      const result: RentalCancelResponse = {
        rentalId,
        status: "CANCELLED",
        cancelReason: "등록자가 거절하였습니다.",
        cancelledAt: new Date().toISOString(),
      };
      return HttpResponse.json(result);
    }
  ),

  // 결제 처리 — POST /api/v1/rentals/:rentalId/payment
  http.post(
    `${BASE_URL}/api/v1/rentals/:rentalId/payment`,
    async ({ params, request }) => {
      await delay(500);
      const rentalId = Number(params.rentalId);
      const body = (await request.json()) as ProcessPaymentRequest;
      const result: ProcessPaymentResponse = {
        rentalId,
        paymentId: 5000 + Math.floor(Math.random() * 1000),
        status: "PAID",
        externalPaymentId: body.paymentKey,
        paidAt: new Date().toISOString(),
      };
      return HttpResponse.json(result);
    }
  ),

  // 대여 시작 — PATCH /api/v1/rentals/:rentalId/start
  http.patch(
    `${BASE_URL}/api/v1/rentals/:rentalId/start`,
    async ({ params }) => {
      await delay(200);
      const rentalId = Number(params.rentalId);
      const result: RentalStartResponse = {
        rentalId,
        status: "IN_USE",
        startedAt: new Date().toISOString(),
      };
      return HttpResponse.json(result);
    }
  ),

  // 반납 처리 — PATCH /api/v1/rentals/:rentalId/return
  http.patch(
    `${BASE_URL}/api/v1/rentals/:rentalId/return`,
    async ({ params }) => {
      await delay(200);
      const rentalId = Number(params.rentalId);
      const result: RentalReturnResponse = {
        rentalId,
        status: "RETURNED",
        returnedAt: new Date().toISOString(),
      };
      return HttpResponse.json(result);
    }
  ),

  // 대여 취소 — PATCH /api/v1/rentals/:rentalId/cancel
  http.patch(
    `${BASE_URL}/api/v1/rentals/:rentalId/cancel`,
    async ({ params, request }) => {
      await delay(200);
      const rentalId = Number(params.rentalId);
      const body = (await request.json()) as RentalRejectRequest;
      const result: RentalCancelResponse = {
        rentalId,
        status: "CANCELLED",
        cancelReason: body.reason,
        cancelledAt: new Date().toISOString(),
      };
      return HttpResponse.json(result);
    }
  ),
];
