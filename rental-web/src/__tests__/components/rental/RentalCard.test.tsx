import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { RentalCard } from "@/components/rental/RentalCard";
import type { Rental } from "@/lib/api/types";

// ========================================
// RentalCard 테스트
// ========================================

const BASE_RENTAL: Rental = {
  id: 1,
  productId: 1,
  productName: "소니 A7C II 미러리스 카메라",
  productImageUrl: "/images/stub/camera.jpg",
  renterId: 2,
  renterName: "홍길동",
  lenderId: 1,
  lenderName: "김등록",
  status: "REQUESTED",
  startDate: "2026-04-20",
  endDate: "2026-04-25",
  dailyPrice: 35000,
  depositAmount: 500000,
  totalAmount: 675000,
  deliveryInfo: {
    recipientName: "홍길동",
    recipientPhone: "010-1234-5678",
    address: "서울시 강남구 테헤란로 123",
    addressDetail: "101호",
    zipCode: "06234",
  },
  requestedAt: "2026-04-14T10:00:00Z",
};

describe("RentalCard", () => {
  it("상품명이 표시되어야 한다", () => {
    render(<RentalCard rental={BASE_RENTAL} />);
    expect(screen.getByTestId("rental-card-product-name")).toHaveTextContent(
      "소니 A7C II 미러리스 카메라"
    );
  });

  it("대여 상태 뱃지가 표시되어야 한다", () => {
    render(<RentalCard rental={BASE_RENTAL} />);
    expect(screen.getByTestId("rental-card-status")).toHaveTextContent("대기 중");
  });

  it("총 금액이 표시되어야 한다", () => {
    render(<RentalCard rental={BASE_RENTAL} />);
    expect(screen.getByText("675,000원")).toBeInTheDocument();
  });

  it("대여 상세 링크가 있어야 한다", () => {
    render(<RentalCard rental={BASE_RENTAL} />);
    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("href", "/rentals/1");
  });

  it("APPROVED 상태는 '승인됨'으로 표시되어야 한다", () => {
    render(<RentalCard rental={{ ...BASE_RENTAL, status: "APPROVED" }} />);
    expect(screen.getByTestId("rental-card-status")).toHaveTextContent("승인됨");
  });

  it("IN_USE 상태는 '대여 중'으로 표시되어야 한다", () => {
    render(<RentalCard rental={{ ...BASE_RENTAL, status: "IN_USE" }} />);
    expect(screen.getByTestId("rental-card-status")).toHaveTextContent("대여 중");
  });

  it("RETURNED 상태는 '반납 완료'로 표시되어야 한다", () => {
    render(<RentalCard rental={{ ...BASE_RENTAL, status: "RETURNED" }} />);
    expect(screen.getByTestId("rental-card-status")).toHaveTextContent("반납 완료");
  });

  it("CANCELLED 상태는 '취소됨'으로 표시되어야 한다", () => {
    render(<RentalCard rental={{ ...BASE_RENTAL, status: "CANCELLED" }} />);
    expect(screen.getByTestId("rental-card-status")).toHaveTextContent("취소됨");
  });

  it("data-testid='rental-card' 속성이 있어야 한다", () => {
    render(<RentalCard rental={BASE_RENTAL} />);
    expect(screen.getByTestId("rental-card")).toBeInTheDocument();
  });
});
