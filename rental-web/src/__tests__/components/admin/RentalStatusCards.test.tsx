import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { RentalStatusCards } from "@/components/admin/RentalStatusCards";

// ========================================
// RentalStatusCards 컴포넌트 테스트
// RC-FE-328
// ========================================

const STUB_COUNTS = {
  REQUESTED: 12,
  APPROVED: 8,
  PAID: 5,
  IN_USE: 23,
  RETURNED: 47,
  CANCELLED: 9,
};

describe("RentalStatusCards", () => {
  it("6개 상태 카드 모두 렌더링", () => {
    render(<RentalStatusCards counts={STUB_COUNTS} />);
    expect(screen.getByTestId("status-card-REQUESTED")).toBeInTheDocument();
    expect(screen.getByTestId("status-card-APPROVED")).toBeInTheDocument();
    expect(screen.getByTestId("status-card-PAID")).toBeInTheDocument();
    expect(screen.getByTestId("status-card-IN_USE")).toBeInTheDocument();
    expect(screen.getByTestId("status-card-RETURNED")).toBeInTheDocument();
    expect(screen.getByTestId("status-card-CANCELLED")).toBeInTheDocument();
  });

  it("각 카드에 올바른 카운트 표시", () => {
    render(<RentalStatusCards counts={STUB_COUNTS} />);
    expect(screen.getByTestId("status-card-IN_USE")).toHaveTextContent("23");
    expect(screen.getByTestId("status-card-RETURNED")).toHaveTextContent("47");
  });

  it("카운트 0인 경우 0 표시", () => {
    render(<RentalStatusCards counts={{}} />);
    expect(screen.getByTestId("status-card-REQUESTED")).toHaveTextContent("0");
  });

  it("한국어 상태 레이블 표시", () => {
    render(<RentalStatusCards counts={STUB_COUNTS} />);
    expect(screen.getByText("대기 중")).toBeInTheDocument();
    expect(screen.getByText("대여 중")).toBeInTheDocument();
    expect(screen.getByText("반납 완료")).toBeInTheDocument();
  });
});
