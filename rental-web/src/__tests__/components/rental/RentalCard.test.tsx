import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { RentalCard } from "@/components/rental/RentalCard";
import type { RentalSummary } from "@/lib/api/types";

// ========================================
// RentalCard 컴포넌트 테스트
// RC-FE-215
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/my-rentals",
}));

const STUB_RENTAL: RentalSummary = {
  rentalId: 1001,
  productId: 4,
  productName: "캠핑 텐트 4인용",
  productThumbnailUrl: "/images/stub/tent.jpg",
  status: "IN_USE",
  startDate: "2026-05-01",
  endDate: "2026-05-07",
  totalAmount: 120000,
  requestedAt: "2026-04-14T10:00:00+09:00",
};

describe("RentalCard", () => {
  it("상품명이 표시되어야 한다", () => {
    render(<RentalCard rental={STUB_RENTAL} />);
    expect(screen.getByText("캠핑 텐트 4인용")).toBeInTheDocument();
  });

  it("금액이 포맷팅되어 표시되어야 한다", () => {
    render(<RentalCard rental={STUB_RENTAL} />);
    expect(screen.getByText("120,000원")).toBeInTheDocument();
  });

  it("IN_USE 상태 뱃지가 주황색으로 표시되어야 한다", () => {
    render(<RentalCard rental={STUB_RENTAL} />);
    const badge = screen.getByTestId("rental-status-badge-1001");
    expect(badge).toHaveTextContent("대여 중");
    expect(badge.className).toContain("bg-orange-100");
  });

  it("APPROVED 상태 뱃지가 파란색으로 표시되어야 한다", () => {
    const approvedRental: RentalSummary = { ...STUB_RENTAL, rentalId: 1002, status: "APPROVED" };
    render(<RentalCard rental={approvedRental} />);
    const badge = screen.getByTestId("rental-status-badge-1002");
    expect(badge).toHaveTextContent("승인됨");
    expect(badge.className).toContain("bg-blue-100");
  });

  it("CANCELLED 상태 뱃지가 빨간색으로 표시되어야 한다", () => {
    const cancelledRental: RentalSummary = { ...STUB_RENTAL, rentalId: 1003, status: "CANCELLED" };
    render(<RentalCard rental={cancelledRental} />);
    const badge = screen.getByTestId("rental-status-badge-1003");
    expect(badge).toHaveTextContent("취소됨");
    expect(badge.className).toContain("bg-red-100");
  });

  it("대여 상세 페이지로 연결되는 링크가 있어야 한다", () => {
    render(<RentalCard rental={STUB_RENTAL} />);
    const card = screen.getByTestId("rental-card-1001");
    expect(card).toHaveAttribute("href", "/rentals/1001");
  });

  it("썸네일 이미지가 없으면 대체 텍스트가 표시되어야 한다", () => {
    const noImageRental: RentalSummary = {
      ...STUB_RENTAL,
      productThumbnailUrl: null,
    };
    render(<RentalCard rental={noImageRental} />);
    expect(screen.getByText("이미지 없음")).toBeInTheDocument();
  });
});
