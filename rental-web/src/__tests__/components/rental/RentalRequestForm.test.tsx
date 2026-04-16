import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RentalRequestForm } from "@/components/rental/RentalRequestForm";
import type { Product } from "@/lib/api/types";

// ========================================
// RentalRequestForm 테스트
// ========================================

// next/navigation mock
vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
  }),
  useSearchParams: () => new URLSearchParams(),
  useParams: () => ({}),
}));

const STUB_PRODUCT: Product = {
  id: 1,
  userId: 1,
  name: "소니 A7C II 미러리스 카메라",
  description: "테스트 상품",
  categoryCode: "ELECTRONICS",
  condition: "GOOD",
  depositAmount: 500000,
  status: "APPROVED",
  prices: [{ id: 1, rentalUnit: "DAILY", priceAmount: 35000 }],
  images: [
    {
      id: 1,
      objectKey: "/images/stub/camera.jpg",
      originalFilename: "camera.jpg",
      sortOrder: 0,
    },
  ],
  createdAt: "2026-03-01T10:00:00Z",
  updatedAt: "2026-03-01T10:00:00Z",
};

describe("RentalRequestForm", () => {
  it("폼이 렌더링되어야 한다", () => {
    render(<RentalRequestForm product={STUB_PRODUCT} />);
    expect(screen.getByTestId("rental-request-form")).toBeInTheDocument();
  });

  it("시작일/종료일 입력 필드가 있어야 한다", () => {
    render(<RentalRequestForm product={STUB_PRODUCT} />);
    expect(screen.getByTestId("input-start-date")).toBeInTheDocument();
    expect(screen.getByTestId("input-end-date")).toBeInTheDocument();
  });

  it("수령인/연락처/주소 입력 필드가 있어야 한다", () => {
    render(<RentalRequestForm product={STUB_PRODUCT} />);
    expect(screen.getByTestId("input-recipient-name")).toBeInTheDocument();
    expect(screen.getByTestId("input-recipient-phone")).toBeInTheDocument();
    expect(screen.getByTestId("input-address")).toBeInTheDocument();
  });

  it("신청하기 버튼이 있어야 한다", () => {
    render(<RentalRequestForm product={STUB_PRODUCT} />);
    expect(screen.getByTestId("submit-button")).toBeInTheDocument();
  });

  it("요금 요약 섹션이 표시되어야 한다", () => {
    render(<RentalRequestForm product={STUB_PRODUCT} />);
    expect(screen.getByTestId("fee-summary")).toBeInTheDocument();
  });

  it("보증금이 표시되어야 한다", () => {
    render(<RentalRequestForm product={STUB_PRODUCT} />);
    expect(screen.getByText("500,000원")).toBeInTheDocument();
  });

  it("빈 폼 제출 시 유효성 에러가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<RentalRequestForm product={STUB_PRODUCT} />);

    await user.click(screen.getByTestId("submit-button"));

    await waitFor(() => {
      expect(screen.getAllByRole("alert").length).toBeGreaterThan(0);
    });
  });

  it("잘못된 전화번호 입력 시 유효성 에러가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    render(<RentalRequestForm product={STUB_PRODUCT} />);

    await user.type(screen.getByTestId("input-recipient-phone"), "01012345678");
    await user.click(screen.getByTestId("submit-button"));

    await waitFor(() => {
      expect(
        screen.getByText("010-XXXX-XXXX 형식으로 입력해 주세요.")
      ).toBeInTheDocument();
    });
  });
});
