import { describe, it, expect, vi, beforeEach } from "vitest";

// ========================================
// Rental API 함수 단위 테스트 (TDD)
// ========================================

const sharedMockClient = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
};

vi.mock("@/lib/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/client")>();
  return {
    ...actual,
    getApiClient: () => sharedMockClient,
  };
});

describe("Rental API 함수", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // TC-01: createRentalApi — 대여 신청
  it("TC-01: createRentalApi — POST /api/v1/rentals 올바른 엔드포인트로 호출", async () => {
    const { createRentalApi } = await import("@/lib/api/rental");
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, status: "REQUESTED" },
      timestamp: new Date().toISOString(),
    });

    const request = {
      productId: 42,
      startDate: "2026-05-01",
      endDate: "2026-05-07",
      deliveryInfo: {
        recipientName: "홍길동",
        recipientPhone: "010-1234-5678",
        addressLine1: "서울특별시 강남구 테헤란로 123",
        zipCode: "06234",
      },
    };

    const result = await createRentalApi(request);

    expect(sharedMockClient.post).toHaveBeenCalledWith(
      "/api/v1/rentals",
      request
    );
    expect(result.data.status).toBe("REQUESTED");
  });

  // TC-02: getRentalsApi — 대여 목록 조회
  it("TC-02: getRentalsApi — GET /api/v1/rentals 올바른 쿼리 파라미터로 호출", async () => {
    const { getRentalsApi } = await import("@/lib/api/rental");
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: {
        content: [],
        number: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
      },
      timestamp: new Date().toISOString(),
    });

    await getRentalsApi({ role: "RENTER", status: "IN_USE", page: 0, size: 20 });

    expect(sharedMockClient.get).toHaveBeenCalledWith("/api/v1/rentals", {
      params: { role: "RENTER", status: "IN_USE", page: "0", size: "20" },
    });
  });

  // TC-03: getRentalByIdApi — 대여 상세 조회
  it("TC-03: getRentalByIdApi — GET /api/v1/rentals/{rentalId} 올바른 경로로 호출", async () => {
    const { getRentalByIdApi } = await import("@/lib/api/rental");
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, status: "IN_USE" },
      timestamp: new Date().toISOString(),
    });

    await getRentalByIdApi(1001);

    expect(sharedMockClient.get).toHaveBeenCalledWith("/api/v1/rentals/1001");
  });

  // TC-04: approveRentalApi — 대여 승인
  it("TC-04: approveRentalApi — PATCH /api/v1/rentals/{rentalId}/approve 호출", async () => {
    const { approveRentalApi } = await import("@/lib/api/rental");
    sharedMockClient.patch.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, status: "APPROVED" },
      timestamp: new Date().toISOString(),
    });

    const result = await approveRentalApi(1001);

    expect(sharedMockClient.patch).toHaveBeenCalledWith(
      "/api/v1/rentals/1001/approve"
    );
    expect(result.data.status).toBe("APPROVED");
  });

  // TC-05: processPaymentApi — 결제 처리
  it("TC-05: processPaymentApi — POST /api/v1/rentals/{rentalId}/payment 올바른 바디로 호출", async () => {
    const { processPaymentApi } = await import("@/lib/api/rental");
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, paymentId: 5001, status: "PAID" },
      timestamp: new Date().toISOString(),
    });

    const paymentRequest = {
      paymentKey: "toss-key-123",
      orderId: "RC-1001-123456",
      amount: 120000,
      paymentMethod: "CARD" as const,
    };

    const result = await processPaymentApi(1001, paymentRequest);

    expect(sharedMockClient.post).toHaveBeenCalledWith(
      "/api/v1/rentals/1001/payment",
      paymentRequest
    );
    expect(result.data.status).toBe("PAID");
  });

  // TC-06: cancelRentalApi — 대여 취소
  it("TC-06: cancelRentalApi — PATCH /api/v1/rentals/{rentalId}/cancel 이유와 함께 호출", async () => {
    const { cancelRentalApi } = await import("@/lib/api/rental");
    sharedMockClient.patch.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, status: "CANCELLED" },
      timestamp: new Date().toISOString(),
    });

    await cancelRentalApi(1001, { reason: "일정이 변경되었습니다." });

    expect(sharedMockClient.patch).toHaveBeenCalledWith(
      "/api/v1/rentals/1001/cancel",
      { reason: "일정이 변경되었습니다." }
    );
  });

  // TC-07: startRentalApi — 대여 시작
  it("TC-07: startRentalApi — PATCH /api/v1/rentals/{rentalId}/start 호출", async () => {
    const { startRentalApi } = await import("@/lib/api/rental");
    sharedMockClient.patch.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, status: "IN_USE" },
      timestamp: new Date().toISOString(),
    });

    const result = await startRentalApi(1001);

    expect(sharedMockClient.patch).toHaveBeenCalledWith(
      "/api/v1/rentals/1001/start"
    );
    expect(result.data.status).toBe("IN_USE");
  });

  // TC-08: returnRentalApi — 반납 처리
  it("TC-08: returnRentalApi — PATCH /api/v1/rentals/{rentalId}/return 호출", async () => {
    const { returnRentalApi } = await import("@/lib/api/rental");
    sharedMockClient.patch.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, status: "RETURNED" },
      timestamp: new Date().toISOString(),
    });

    const result = await returnRentalApi(1001);

    expect(sharedMockClient.patch).toHaveBeenCalledWith(
      "/api/v1/rentals/1001/return"
    );
    expect(result.data.status).toBe("RETURNED");
  });

  // TC-09: rejectRentalApi — 대여 거절
  it("TC-09: rejectRentalApi — PATCH /api/v1/rentals/{rentalId}/reject 이유와 함께 호출", async () => {
    const { rejectRentalApi } = await import("@/lib/api/rental");
    sharedMockClient.patch.mockResolvedValue({
      success: true,
      data: { rentalId: 1001, status: "CANCELLED" },
      timestamp: new Date().toISOString(),
    });

    await rejectRentalApi(1001, { reason: "해당 기간에 다른 일정이 있습니다." });

    expect(sharedMockClient.patch).toHaveBeenCalledWith(
      "/api/v1/rentals/1001/reject",
      { reason: "해당 기간에 다른 일정이 있습니다." }
    );
  });
});
