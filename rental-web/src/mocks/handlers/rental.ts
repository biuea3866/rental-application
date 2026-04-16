import { http, HttpResponse, delay } from "msw";
import {
  STUB_RENTAL_SUMMARIES,
  STUB_RENTAL_DETAILS,
  filterRentalsByStatus,
  findRentalById,
} from "../rentals";
import type {
  RentalStatus,
  RentalSummary,
  RejectOrCancelRentalRequest,
  ProcessPaymentRequest,
  RentalDetail,
} from "@/lib/api/types";

// ========================================
// 대여 MSW 핸들러 (BE TDD-002 기준)
// 독립적 상태 관리 — RC-FE-214 핸들러와 충돌 없음
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 테스트 격리를 위한 인메모리 상태
let rentalSummaryState: RentalSummary[] = [...STUB_RENTAL_SUMMARIES];
let rentalDetailState: RentalDetail[] = [...STUB_RENTAL_DETAILS];
let paymentIdCounter = 5100;

export function resetRentalHandlerState(): void {
  rentalSummaryState = [...STUB_RENTAL_SUMMARIES];
  rentalDetailState = [...STUB_RENTAL_DETAILS];
  paymentIdCounter = 5100;
}

export const rentalHandlers = [
  // GET /api/v1/rentals — 내 대여 목록 조회
  http.get(`${BASE_URL}/api/v1/rentals`, async ({ request }) => {
    await delay(200);

    const url = new URL(request.url);
    const status = url.searchParams.get("status") as RentalStatus | null;
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    const filtered = filterRentalsByStatus(rentalSummaryState, status ?? undefined);

    const start = page * size;
    const end = start + size;
    const paginated = filtered.slice(start, end);

    return HttpResponse.json({
      success: true,
      data: {
        content: paginated,
        totalElements: filtered.length,
        totalPages: Math.ceil(filtered.length / size),
        size,
        number: page,
      },
      timestamp: new Date().toISOString(),
    });
  }),

  // GET /api/v1/rentals/:rentalId — 대여 상세 조회
  http.get(`${BASE_URL}/api/v1/rentals/:rentalId`, async ({ params }) => {
    await delay(200);

    const rentalId = parseInt(params.rentalId as string, 10);
    const detail = findRentalById(rentalId);

    if (!detail) {
      return HttpResponse.json(
        {
          success: false,
          code: "RENTAL_NOT_FOUND",
          message: "대여 정보를 찾을 수 없습니다.",
        },
        { status: 404 }
      );
    }

    const current = rentalDetailState.find((r) => r.rentalId === rentalId) ?? detail;

    return HttpResponse.json({
      success: true,
      data: current,
      timestamp: new Date().toISOString(),
    });
  }),

  // POST /api/v1/rentals — 대여 신청
  http.post(`${BASE_URL}/api/v1/rentals`, async ({ request }) => {
    await delay(300);

    const body = await request.json() as {
      productId: number;
      startDate: string;
      endDate: string;
    };

    const newRentalId = 1000 + rentalSummaryState.length + 1;
    const now = new Date().toISOString();

    const newSummary: RentalSummary = {
      rentalId: newRentalId,
      productId: body.productId,
      productName: "테스트 상품",
      productThumbnailUrl: null,
      status: "REQUESTED",
      startDate: body.startDate,
      endDate: body.endDate,
      totalAmount: 100000,
      requestedAt: now,
    };

    rentalSummaryState.push(newSummary);

    return HttpResponse.json(
      {
        success: true,
        data: {
          rentalId: newRentalId,
          productId: body.productId,
          status: "REQUESTED",
          startDate: body.startDate,
          endDate: body.endDate,
          totalAmount: 100000,
          depositAmount: 50000,
          requestedAt: now,
        },
        timestamp: now,
      },
      { status: 201 }
    );
  }),

  // PATCH /api/v1/rentals/:rentalId/approve — 승인
  http.patch(`${BASE_URL}/api/v1/rentals/:rentalId/approve`, async ({ params }) => {
    await delay(200);

    const rentalId = parseInt(params.rentalId as string, 10);
    const detail = rentalDetailState.find((r) => r.rentalId === rentalId);

    if (!detail) {
      return HttpResponse.json(
        { success: false, code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (detail.status !== "REQUESTED") {
      return HttpResponse.json(
        { success: false, code: "INVALID_STATUS_TRANSITION", message: "REQUESTED 상태에서만 승인할 수 있습니다." },
        { status: 409 }
      );
    }

    const now = new Date().toISOString();
    detail.status = "APPROVED";
    detail.approvedAt = now;

    const summary = rentalSummaryState.find((r) => r.rentalId === rentalId);
    if (summary) summary.status = "APPROVED";

    return HttpResponse.json({
      success: true,
      data: { rentalId, status: "APPROVED", approvedAt: now },
      timestamp: now,
    });
  }),

  // PATCH /api/v1/rentals/:rentalId/reject — 거절
  http.patch(`${BASE_URL}/api/v1/rentals/:rentalId/reject`, async ({ params, request }) => {
    await delay(200);

    const rentalId = parseInt(params.rentalId as string, 10);
    const body = await request.json() as RejectOrCancelRentalRequest;
    const detail = rentalDetailState.find((r) => r.rentalId === rentalId);

    if (!detail) {
      return HttpResponse.json(
        { success: false, code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (detail.status !== "REQUESTED") {
      return HttpResponse.json(
        { success: false, code: "INVALID_STATUS_TRANSITION", message: "REQUESTED 상태에서만 거절할 수 있습니다." },
        { status: 409 }
      );
    }

    const now = new Date().toISOString();
    detail.status = "CANCELLED";
    detail.cancelReason = body.reason;
    detail.cancelledAt = now;

    const summary = rentalSummaryState.find((r) => r.rentalId === rentalId);
    if (summary) summary.status = "CANCELLED";

    return HttpResponse.json({
      success: true,
      data: { rentalId, status: "CANCELLED", cancelReason: body.reason, cancelledAt: now },
      timestamp: now,
    });
  }),

  // POST /api/v1/rentals/:rentalId/payment — 결제
  http.post(`${BASE_URL}/api/v1/rentals/:rentalId/payment`, async ({ params, request }) => {
    await delay(500);

    const rentalId = parseInt(params.rentalId as string, 10);
    const body = await request.json() as ProcessPaymentRequest;
    const detail = rentalDetailState.find((r) => r.rentalId === rentalId);

    if (!detail) {
      return HttpResponse.json(
        { success: false, code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (detail.status !== "APPROVED") {
      return HttpResponse.json(
        { success: false, code: "INVALID_STATUS_TRANSITION", message: "APPROVED 상태에서만 결제할 수 있습니다." },
        { status: 409 }
      );
    }

    const now = new Date().toISOString();
    const newPaymentId = paymentIdCounter++;
    detail.status = "PAID";
    detail.paidAt = now;
    detail.payment = {
      paymentId: newPaymentId,
      amount: body.amount,
      paymentMethod: body.paymentMethod,
      status: "COMPLETED",
      paidAt: now,
    };

    const summary = rentalSummaryState.find((r) => r.rentalId === rentalId);
    if (summary) summary.status = "PAID";

    return HttpResponse.json({
      success: true,
      data: {
        rentalId,
        paymentId: newPaymentId,
        status: "PAID",
        externalPaymentId: body.paymentKey,
        paidAt: now,
      },
      timestamp: now,
    });
  }),

  // PATCH /api/v1/rentals/:rentalId/start — 배송 시작
  http.patch(`${BASE_URL}/api/v1/rentals/:rentalId/start`, async ({ params }) => {
    await delay(200);

    const rentalId = parseInt(params.rentalId as string, 10);
    const detail = rentalDetailState.find((r) => r.rentalId === rentalId);

    if (!detail) {
      return HttpResponse.json(
        { success: false, code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (detail.status !== "PAID") {
      return HttpResponse.json(
        { success: false, code: "INVALID_STATUS_TRANSITION", message: "PAID 상태에서만 배송 시작할 수 있습니다." },
        { status: 409 }
      );
    }

    const now = new Date().toISOString();
    detail.status = "IN_USE";
    detail.startedAt = now;

    const summary = rentalSummaryState.find((r) => r.rentalId === rentalId);
    if (summary) summary.status = "IN_USE";

    return HttpResponse.json({
      success: true,
      data: { rentalId, status: "IN_USE", startedAt: now },
      timestamp: now,
    });
  }),

  // PATCH /api/v1/rentals/:rentalId/return — 반납 처리
  http.patch(`${BASE_URL}/api/v1/rentals/:rentalId/return`, async ({ params }) => {
    await delay(200);

    const rentalId = parseInt(params.rentalId as string, 10);
    const detail = rentalDetailState.find((r) => r.rentalId === rentalId);

    if (!detail) {
      return HttpResponse.json(
        { success: false, code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (detail.status !== "IN_USE") {
      return HttpResponse.json(
        { success: false, code: "INVALID_STATUS_TRANSITION", message: "IN_USE 상태에서만 반납할 수 있습니다." },
        { status: 409 }
      );
    }

    const now = new Date().toISOString();
    detail.status = "RETURNED";
    detail.returnedAt = now;

    const summary = rentalSummaryState.find((r) => r.rentalId === rentalId);
    if (summary) summary.status = "RETURNED";

    return HttpResponse.json({
      success: true,
      data: { rentalId, status: "RETURNED", returnedAt: now },
      timestamp: now,
    });
  }),

  // PATCH /api/v1/rentals/:rentalId/cancel — 취소
  http.patch(`${BASE_URL}/api/v1/rentals/:rentalId/cancel`, async ({ params, request }) => {
    await delay(200);

    const rentalId = parseInt(params.rentalId as string, 10);
    const body = await request.json() as RejectOrCancelRentalRequest;
    const detail = rentalDetailState.find((r) => r.rentalId === rentalId);

    if (!detail) {
      return HttpResponse.json(
        { success: false, code: "RENTAL_NOT_FOUND", message: "대여를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (detail.status !== "REQUESTED" && detail.status !== "APPROVED") {
      return HttpResponse.json(
        { success: false, code: "INVALID_STATUS_TRANSITION", message: "REQUESTED/APPROVED 상태에서만 취소할 수 있습니다." },
        { status: 409 }
      );
    }

    const now = new Date().toISOString();
    detail.status = "CANCELLED";
    detail.cancelReason = body.reason;
    detail.cancelledAt = now;

    const summary = rentalSummaryState.find((r) => r.rentalId === rentalId);
    if (summary) summary.status = "CANCELLED";

    return HttpResponse.json({
      success: true,
      data: { rentalId, status: "CANCELLED", cancelReason: body.reason, cancelledAt: now },
      timestamp: now,
    });
  }),
];
