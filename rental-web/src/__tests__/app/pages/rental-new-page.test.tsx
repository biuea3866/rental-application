import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// ========================================
// 대여 신청 페이지 테스트 (TDD)
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  useParams: () => ({ id: "42" }),
  useSearchParams: () => new URLSearchParams("productId=42"),
}));

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

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0, gcTime: 0 },
    },
  });
}

function renderWithQuery(ui: React.ReactElement) {
  const queryClient = createQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
  );
}

const mockProduct = {
  id: 42,
  userId: 22,
  name: "캠핑 텐트 A",
  description: "좋은 텐트",
  categoryCode: "CAMPING",
  condition: "GOOD" as const,
  status: "APPROVED" as const,
  depositAmount: 50000,
  prices: [{ id: 1, rentalUnit: "DAILY" as const, priceAmount: 10000 }],
  images: [],
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("대여 신청 페이지 (/rentals/new)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // TC-10: 페이지 제목이 렌더링되어야 한다
  it("TC-10: '대여 신청' 제목이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockProduct,
      timestamp: new Date().toISOString(),
    });

    const RentalNewPage = (
      await import("@/app/rentals/new/page")
    ).default;
    renderWithQuery(<RentalNewPage />);

    await waitFor(() => {
      expect(screen.getByText("대여 신청")).toBeInTheDocument();
    });
  });

  // TC-11: 상품 정보가 표시되어야 한다
  it("TC-11: 상품명이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockProduct,
      timestamp: new Date().toISOString(),
    });

    const RentalNewPage = (
      await import("@/app/rentals/new/page")
    ).default;
    renderWithQuery(<RentalNewPage />);

    await waitFor(() => {
      expect(screen.getByText("캠핑 텐트 A")).toBeInTheDocument();
    });
  });

  // TC-12: 기간 입력 필드가 존재해야 한다
  it("TC-12: 시작일, 종료일 입력 필드가 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockProduct,
      timestamp: new Date().toISOString(),
    });

    const RentalNewPage = (
      await import("@/app/rentals/new/page")
    ).default;
    renderWithQuery(<RentalNewPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("시작일")).toBeInTheDocument();
      expect(screen.getByLabelText("종료일")).toBeInTheDocument();
    });
  });

  // TC-13: 배송지 입력 필드들이 존재해야 한다
  it("TC-13: 수령인, 연락처, 주소 입력 필드가 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockProduct,
      timestamp: new Date().toISOString(),
    });

    const RentalNewPage = (
      await import("@/app/rentals/new/page")
    ).default;
    renderWithQuery(<RentalNewPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("수령인")).toBeInTheDocument();
      expect(screen.getByLabelText("연락처")).toBeInTheDocument();
      expect(screen.getByLabelText("주소")).toBeInTheDocument();
    });
  });

  // TC-14: 신청하기 버튼이 존재해야 한다
  it("TC-14: 신청하기 버튼이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockProduct,
      timestamp: new Date().toISOString(),
    });

    const RentalNewPage = (
      await import("@/app/rentals/new/page")
    ).default;
    renderWithQuery(<RentalNewPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "신청하기" })
      ).toBeInTheDocument();
    });
  });

  // TC-15: 폼 제출 시 createRentalApi 호출
  it("TC-15: 폼 유효성 통과 후 POST /api/v1/rentals가 호출된다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockProduct,
      timestamp: new Date().toISOString(),
    });
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: {
        rentalId: 2001,
        productId: 42,
        status: "REQUESTED",
        startDate: "2026-05-01",
        endDate: "2026-05-07",
        totalAmount: 70000,
        depositAmount: 50000,
        requestedAt: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    });

    const RentalNewPage = (
      await import("@/app/rentals/new/page")
    ).default;
    renderWithQuery(<RentalNewPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("시작일")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("시작일"), {
      target: { value: "2026-05-01" },
    });
    fireEvent.change(screen.getByLabelText("종료일"), {
      target: { value: "2026-05-07" },
    });
    fireEvent.change(screen.getByLabelText("수령인"), {
      target: { value: "홍길동" },
    });
    fireEvent.change(screen.getByLabelText("연락처"), {
      target: { value: "010-1234-5678" },
    });
    fireEvent.change(screen.getByLabelText("주소"), {
      target: { value: "서울특별시 강남구 테헤란로 123" },
    });

    fireEvent.click(screen.getByRole("button", { name: "신청하기" }));

    await waitFor(() => {
      expect(sharedMockClient.post).toHaveBeenCalledWith(
        "/api/v1/rentals",
        expect.objectContaining({ productId: 42 })
      );
    });
  });
});
