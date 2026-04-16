import { describe, it, expect } from "vitest";
import {
  createRentalApi,
  getMyRentalsApi,
  getRentalDetailApi,
  approveRentalApi,
  rejectRentalApi,
  cancelRentalApi,
  processPaymentApi,
  startRentalApi,
  returnRentalApi,
} from "@/lib/api/rental";

// ========================================
// Rental API 테스트 (MSW 핸들러 사용)
// ========================================

describe("createRentalApi", () => {
  it("대여 신청이 성공해야 한다", async () => {
    const res = await createRentalApi({
      productId: 1,
      startDate: "2026-05-01",
      endDate: "2026-05-05",
      deliveryInfo: {
        recipientName: "홍길동",
        recipientPhone: "010-1234-5678",
        address: "서울시 강남구 테헤란로 123",
        addressDetail: "101호",
        zipCode: "06234",
      },
    });

    expect(res.data.rentalId).toBeGreaterThan(0);
    expect(res.data.status).toBe("REQUESTED");
  });

  it("존재하지 않는 상품으로 신청 시 에러가 발생해야 한다", async () => {
    await expect(
      createRentalApi({
        productId: 9999,
        startDate: "2026-05-01",
        endDate: "2026-05-05",
        deliveryInfo: {
          recipientName: "홍길동",
          recipientPhone: "010-1234-5678",
          address: "서울시 강남구 테헤란로 123",
          addressDetail: "",
          zipCode: "06234",
        },
      })
    ).rejects.toThrow();
  });
});

describe("getMyRentalsApi", () => {
  it("대여자로서 내 대여 목록을 조회할 수 있어야 한다", async () => {
    const res = await getMyRentalsApi({ role: "renter" });
    expect(Array.isArray(res.data.content)).toBe(true);
    expect(res.data.content.length).toBeGreaterThan(0);
  });

  it("등록자로서 대여 요청 목록을 조회할 수 있어야 한다", async () => {
    const res = await getMyRentalsApi({ role: "lender" });
    expect(Array.isArray(res.data.content)).toBe(true);
  });

  it("role 파라미터 없이 조회할 수 있어야 한다 (기본: renter)", async () => {
    const res = await getMyRentalsApi();
    expect(Array.isArray(res.data.content)).toBe(true);
  });
});

describe("getRentalDetailApi", () => {
  it("대여 상세 정보를 조회할 수 있어야 한다", async () => {
    const res = await getRentalDetailApi(1);
    expect(res.data.id).toBe(1);
    expect(res.data.productName).toBeTruthy();
    expect(res.data.status).toBeTruthy();
  });

  it("존재하지 않는 대여 조회 시 에러가 발생해야 한다", async () => {
    await expect(getRentalDetailApi(9999)).rejects.toThrow();
  });
});

describe("approveRentalApi", () => {
  it("REQUESTED 상태 대여를 승인할 수 있어야 한다", async () => {
    const res = await approveRentalApi(1);
    expect(res.data.status).toBe("APPROVED");
  });
});

describe("rejectRentalApi", () => {
  it("REQUESTED 상태 대여를 거절할 수 있어야 한다", async () => {
    const res = await rejectRentalApi(1, {
      reason: "해당 기간에 상품 사용이 불가합니다.",
    });
    expect(res.data.status).toBe("CANCELLED");
    expect(res.data.cancelReason).toBeTruthy();
  });
});

describe("cancelRentalApi", () => {
  it("REQUESTED 상태 대여를 취소할 수 있어야 한다", async () => {
    const res = await cancelRentalApi(1, { reason: "일정이 변경되었습니다." });
    expect(res.data.status).toBe("CANCELLED");
  });

  it("APPROVED 상태 대여를 취소할 수 있어야 한다", async () => {
    const res = await cancelRentalApi(2);
    expect(res.data.status).toBe("CANCELLED");
  });
});

describe("processPaymentApi", () => {
  it("APPROVED 상태 대여에 결제를 처리할 수 있어야 한다", async () => {
    const res = await processPaymentApi(2, {
      paymentKey: "toss-test-key-abc",
      orderId: "RC-2-12345",
      amount: 1150000,
    });
    expect(res.data.status).toBe("PAID");
    expect(res.data.payment).toBeTruthy();
    expect(res.data.payment?.status).toBe("COMPLETED");
  });
});

describe("startRentalApi", () => {
  it("PAID 상태 대여의 배송을 시작할 수 있어야 한다", async () => {
    const res = await startRentalApi(3);
    expect(res.data.status).toBe("IN_USE");
  });
});

describe("returnRentalApi", () => {
  it("IN_USE 상태 대여를 반납 처리할 수 있어야 한다", async () => {
    const res = await returnRentalApi(4);
    expect(res.data.status).toBe("RETURNED");
  });
});
