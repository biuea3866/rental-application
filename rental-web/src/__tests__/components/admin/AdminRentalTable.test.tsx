import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { AdminRentalTable } from "@/components/admin/AdminRentalTable";
import type { AdminRentalResponse } from "@/lib/api/types";

// ========================================
// AdminRentalTable 컴포넌트 테스트
// RC-FE-329
// ========================================

const STUB_RENTALS: AdminRentalResponse[] = [
  {
    rentalId: 1001,
    renterId: 11,
    lenderId: 22,
    productId: 42,
    productName: "캠핑 텐트 A",
    status: "IN_USE",
    totalAmount: 120000,
    startDate: "2026-05-01",
    endDate: "2026-05-07",
    createdAt: "2026-04-14T10:00:00+09:00",
  },
  {
    rentalId: 1002,
    renterId: 11,
    lenderId: 33,
    productId: 43,
    productName: "소니 카메라",
    status: "REQUESTED",
    totalAmount: 75000,
    startDate: "2026-05-10",
    endDate: "2026-05-15",
    createdAt: "2026-04-14T11:00:00+09:00",
  },
];

describe("AdminRentalTable", () => {
  it("대여 항목 렌더링", () => {
    render(<AdminRentalTable rentals={STUB_RENTALS} />);
    expect(screen.getByTestId("rental-row-1001")).toBeInTheDocument();
    expect(screen.getByTestId("rental-row-1002")).toBeInTheDocument();
  });

  it("빈 데이터 시 빈 상태 메시지 표시", () => {
    render(<AdminRentalTable rentals={[]} />);
    expect(screen.getByTestId("rental-table-empty")).toBeInTheDocument();
  });

  it("로딩 상태 표시", () => {
    render(<AdminRentalTable rentals={[]} isLoading />);
    expect(screen.queryByTestId("rental-table-empty")).not.toBeInTheDocument();
  });

  it("상태 필터 버튼 클릭 시 콜백 호출", () => {
    const onStatusFilterChange = vi.fn();
    render(
      <AdminRentalTable
        rentals={STUB_RENTALS}
        onStatusFilterChange={onStatusFilterChange}
      />
    );
    fireEvent.click(screen.getByTestId("rental-filter-IN_USE"));
    expect(onStatusFilterChange).toHaveBeenCalledWith("IN_USE");
  });

  it("전체 필터 버튼 클릭 시 undefined 전달", () => {
    const onStatusFilterChange = vi.fn();
    render(
      <AdminRentalTable
        rentals={STUB_RENTALS}
        onStatusFilterChange={onStatusFilterChange}
      />
    );
    fireEvent.click(screen.getByTestId("rental-filter-all"));
    expect(onStatusFilterChange).toHaveBeenCalledWith(undefined);
  });
});
