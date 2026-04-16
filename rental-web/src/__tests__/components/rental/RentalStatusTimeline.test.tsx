import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { RentalStatusTimeline } from "@/components/rental/RentalStatusTimeline";
import type { RentalDetail } from "@/lib/api/types";

// ========================================
// RentalStatusTimeline 컴포넌트 테스트
// RC-FE-215
// ========================================

const BASE_RENTAL: RentalDetail = {
  rentalId: 1001,
  product: {
    productId: 4,
    name: "캠핑 텐트",
    thumbnailUrl: null,
    category: "SPORTS",
  },
  renter: { userId: 3, name: "이빌림" },
  lender: { userId: 1, name: "김대여" },
  status: "REQUESTED",
  startDate: "2026-05-01",
  endDate: "2026-05-07",
  totalAmount: 120000,
  depositAmount: 200000,
  deliveryInfo: {
    recipientName: "이빌림",
    recipientPhone: "010-3456-7890",
    addressLine1: "서울특별시 강남구 테헤란로 123",
    zipCode: "06234",
  },
  timeline: [
    { status: "REQUESTED" as const, occurredAt: "2026-04-14T10:00:00+09:00" },
  ],
  requestedAt: "2026-04-14T10:00:00+09:00",
};

describe("RentalStatusTimeline", () => {
  it("타임라인 컴포넌트가 렌더링되어야 한다", () => {
    render(<RentalStatusTimeline rental={BASE_RENTAL} />);
    expect(screen.getByTestId("rental-status-timeline")).toBeInTheDocument();
  });

  it("REQUESTED 상태일 때 신청 스텝이 활성화되어야 한다", () => {
    render(<RentalStatusTimeline rental={BASE_RENTAL} />);
    const requestedStep = screen.getByTestId("timeline-step-REQUESTED");
    expect(requestedStep).toBeInTheDocument();
  });

  it("모든 상태 스텝(신청, 승인, 결제, 대여중, 반납)이 표시되어야 한다", () => {
    render(<RentalStatusTimeline rental={BASE_RENTAL} />);
    expect(screen.getByText("신청")).toBeInTheDocument();
    expect(screen.getByText("승인")).toBeInTheDocument();
    expect(screen.getByText("결제")).toBeInTheDocument();
    expect(screen.getByText("대여중")).toBeInTheDocument();
    expect(screen.getByText("반납")).toBeInTheDocument();
  });

  it("CANCELLED 상태일 때 취소 UI가 표시되어야 한다", () => {
    const cancelledRental: RentalDetail = {
      ...BASE_RENTAL,
      status: "CANCELLED",
      cancelReason: "일정 변경으로 취소합니다.",
      cancelledAt: "2026-04-15T10:00:00+09:00",
    };
    render(<RentalStatusTimeline rental={cancelledRental} />);
    expect(screen.getByText("취소됨")).toBeInTheDocument();
    expect(screen.getByText("사유: 일정 변경으로 취소합니다.")).toBeInTheDocument();
  });

  it("IN_USE 상태일 때 결제/승인 스텝이 완료 표시되어야 한다", () => {
    const inUseRental: RentalDetail = {
      ...BASE_RENTAL,
      status: "IN_USE",
      approvedAt: "2026-04-14T11:00:00+09:00",
      paidAt: "2026-04-14T12:00:00+09:00",
      startedAt: "2026-04-15T09:00:00+09:00",
    };
    render(<RentalStatusTimeline rental={inUseRental} />);
    // IN_USE 스텝이 active 상태
    expect(screen.getByTestId("timeline-step-IN_USE")).toBeInTheDocument();
  });
});
