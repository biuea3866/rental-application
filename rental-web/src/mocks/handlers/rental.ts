import { http, HttpResponse, delay } from "msw";
import {
  stubRentals,
  findRentalById,
  findRentalsByRole,
  updateRentalStatus,
  generateRentalId,
  resetStubRentals,
} from "../rentals";
import { STUB_PRODUCTS } from "../products";
import type {
  CreateRentalRequest,
  RentalPaymentRequest,
  Rental,
  RentalStatus,
} from "@/lib/api/types";

// ========================================
// Rental MSW 핸들러 (BE 스키마 기준)
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export function resetRentalHandlerState(): void {
  resetStubRentals();
}

export const rentalHandlers = [
  // 대여 신청 (POST /api/v1/rentals)
  http.post(`${BASE_URL}/api/v1/rentals`, async ({ request }) => {
    await delay(200);

    const body = (await request.json()) as CreateRentalRequest;
    const product = STUB_PRODUCTS.find((p) => p.id === body.productId);

    if (!product) {
      return HttpResponse.json(
        { code: "PRODUCT_NOT_FOUND", message: "상품을 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (product.status !== "APPROVED") {
      return HttpResponse.json(
        {
          code: "PRODUCT_NOT_AVAILABLE",
          message: "현재 대여 신청 불가한 상품입니다.",
        },
        { status: 409 }
      );
    }

    const dailyPrice =
      product.prices.find((p) => p.rentalUnit === "DAILY")?.priceAmount ??
      product.prices[0]?.priceAmount ??
      0;

    const start = new Date(body.startDate);
    const end = new Date(body.endDate);
    const days = Math.max(
      1,
      Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
    );

    const newRental: Rental = {
      id: generateRentalId(),
      productId: body.productId,
      productName: product.name ?? "상품명 없음",
      productImageUrl: product.images[0]?.objectKey,
      renterId: 2,
      renterName: "홍길동",
      lenderId: product.userId,
      lenderName: "김등록",
      status: "REQUESTED",
      startDate: body.startDate,
      endDate: body.endDate,
      dailyPrice,
      depositAmount: product.depositAmount ?? 0,
      totalAmount: dailyPrice * days + (product.depositAmount ?? 0),
      deliveryInfo: body.deliveryInfo,
      requestedAt: new Date().toISOString(),
    };

    stubRentals.push(newRental);

    return HttpResponse.json(
      { rentalId: newRental.id, status: newRental.status },
      { status: 201 }
    );
  }),

  // 내 대여 목록 (GET /api/v1/rentals/mine)
  http.get(`${BASE_URL}/api/v1/rentals/mine`, async ({ request }) => {
    await delay(200);

    const url = new URL(request.url);
    const role = (url.searchParams.get("role") as "renter" | "lender") ?? "renter";
    const statusFilter = url.searchParams.get("status") as RentalStatus | null;
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    let rentals = findRentalsByRole(role);
    if (statusFilter) {
      rentals = rentals.filter((r) => r.status === statusFilter);
    }

    const totalElements = rentals.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const start = page * size;
    const content = rentals.slice(start, start + size);
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

  // 대여 상세 (GET /api/v1/rentals/:id)
  http.get(`${BASE_URL}/api/v1/rentals/:id`, async ({ params }) => {
    await delay(150);

    const rental = findRentalById(params.id as string);
    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여 정보를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    return HttpResponse.json(rental);
  }),

  // 대여 승인 (PATCH /api/v1/rentals/:id/approve)
  http.patch(`${BASE_URL}/api/v1/rentals/:id/approve`, async ({ params }) => {
    await delay(200);

    const rental = findRentalById(params.id as string);
    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여 정보를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (rental.status !== "REQUESTED") {
      return HttpResponse.json(
        { code: "INVALID_STATUS", message: "승인 가능한 상태가 아닙니다." },
        { status: 400 }
      );
    }

    const updated = updateRentalStatus(params.id as string, "APPROVED", {
      approvedAt: new Date().toISOString(),
    });

    return HttpResponse.json(updated);
  }),

  // 대여 거절 (PATCH /api/v1/rentals/:id/reject)
  http.patch(`${BASE_URL}/api/v1/rentals/:id/reject`, async ({ params, request }) => {
    await delay(200);

    const body = (await request.json()) as { reason: string };
    const rental = findRentalById(params.id as string);

    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여 정보를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (rental.status !== "REQUESTED") {
      return HttpResponse.json(
        { code: "INVALID_STATUS", message: "거절 가능한 상태가 아닙니다." },
        { status: 400 }
      );
    }

    const updated = updateRentalStatus(params.id as string, "CANCELLED", {
      cancelReason: body.reason,
      cancelledAt: new Date().toISOString(),
    });

    return HttpResponse.json(updated);
  }),

  // 대여 취소 (PATCH /api/v1/rentals/:id/cancel)
  http.patch(`${BASE_URL}/api/v1/rentals/:id/cancel`, async ({ params, request }) => {
    await delay(200);

    let reason: string | undefined;
    try {
      const body = (await request.json()) as { reason?: string };
      reason = body.reason;
    } catch {
      // body가 없는 경우 무시
    }

    const rental = findRentalById(params.id as string);
    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여 정보를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (!["REQUESTED", "APPROVED"].includes(rental.status)) {
      return HttpResponse.json(
        { code: "INVALID_STATUS", message: "취소 가능한 상태가 아닙니다." },
        { status: 400 }
      );
    }

    const updated = updateRentalStatus(params.id as string, "CANCELLED", {
      cancelReason: reason,
      cancelledAt: new Date().toISOString(),
    });

    return HttpResponse.json(updated);
  }),

  // 결제 처리 (POST /api/v1/rentals/:id/payment)
  http.post(`${BASE_URL}/api/v1/rentals/:id/payment`, async ({ params, request }) => {
    await delay(300);

    const body = (await request.json()) as RentalPaymentRequest;
    const rental = findRentalById(params.id as string);

    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여 정보를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (rental.status !== "APPROVED") {
      return HttpResponse.json(
        { code: "INVALID_STATUS", message: "결제 가능한 상태가 아닙니다." },
        { status: 400 }
      );
    }

    const updated = updateRentalStatus(params.id as string, "PAID", {
      payment: {
        id: Date.now(),
        paymentKey: body.paymentKey,
        orderId: body.orderId,
        amount: body.amount,
        status: "COMPLETED",
        paymentMethod: "카드",
        paidAt: new Date().toISOString(),
      },
      paidAt: new Date().toISOString(),
    });

    return HttpResponse.json(updated);
  }),

  // 배송 시작 (PATCH /api/v1/rentals/:id/start)
  http.patch(`${BASE_URL}/api/v1/rentals/:id/start`, async ({ params }) => {
    await delay(200);

    const rental = findRentalById(params.id as string);
    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여 정보를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (rental.status !== "PAID") {
      return HttpResponse.json(
        { code: "INVALID_STATUS", message: "배송 시작 가능한 상태가 아닙니다." },
        { status: 400 }
      );
    }

    const updated = updateRentalStatus(params.id as string, "IN_USE", {
      startedAt: new Date().toISOString(),
    });

    return HttpResponse.json(updated);
  }),

  // 반납 처리 (PATCH /api/v1/rentals/:id/return)
  http.patch(`${BASE_URL}/api/v1/rentals/:id/return`, async ({ params }) => {
    await delay(200);

    const rental = findRentalById(params.id as string);
    if (!rental) {
      return HttpResponse.json(
        { code: "RENTAL_NOT_FOUND", message: "대여 정보를 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    if (rental.status !== "IN_USE") {
      return HttpResponse.json(
        { code: "INVALID_STATUS", message: "반납 처리 가능한 상태가 아닙니다." },
        { status: 400 }
      );
    }

    const updated = updateRentalStatus(params.id as string, "RETURNED", {
      returnedAt: new Date().toISOString(),
    });

    return HttpResponse.json(updated);
  }),
];
