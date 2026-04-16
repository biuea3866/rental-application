import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// ========================================
// 결제 페이지 테스트 (TDD)
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  useParams: () => ({ id: "1003" }),
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

const mockApprovedRental = {
  rentalId: 1003,
  product: {
    productId: 44,
    name: "캠핑 의자 세트",
    thumbnailUrl: null,
    category: "CAMPING",
  },
  renter: { userId: 11, name: "홍길동" },
  lender: { userId: 22, name: "김철수" },
  status: "APPROVED",
  startDate: "2026-05-20",
  endDate: "2026-05-22",
  totalAmount: 30000,
  depositAmount: 10000,
  deliveryInfo: {
    recipientName: "홍길동",
    recipientPhone: "010-1234-5678",
    addressLine1: "서울특별시 강남구 테헤란로 123",
    addressLine2: "101호",
    zipCode: "06234",
  },
  timeline: [
    { status: "REQUESTED", occurredAt: "2026-04-13T09:00:00+09:00" },
    { status: "APPROVED", occurredAt: "2026-04-13T10:00:00+09:00" },
  ],
  requestedAt: "2026-04-13T09:00:00+09:00",
  approvedAt: "2026-04-13T10:00:00+09:00",
};

describe("결제 페이지 (/rentals/[id]/payment)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // TC-16: '결제' 제목이 렌더링되어야 한다
  it("TC-16: '결제' 제목이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockApprovedRental,
      timestamp: new Date().toISOString(),
    });

    const PaymentPage = (
      await import("@/app/rentals/[id]/payment/page")
    ).default;
    renderWithQuery(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText("결제")).toBeInTheDocument();
    });
  });

  // TC-17: 상품명이 표시되어야 한다
  it("TC-17: 대여 상품명이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockApprovedRental,
      timestamp: new Date().toISOString(),
    });

    const PaymentPage = (
      await import("@/app/rentals/[id]/payment/page")
    ).default;
    renderWithQuery(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText("캠핑 의자 세트")).toBeInTheDocument();
    });
  });

  // TC-18: 결제 금액이 표시되어야 한다
  it("TC-18: 대여료와 보증금이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockApprovedRental,
      timestamp: new Date().toISOString(),
    });

    const PaymentPage = (
      await import("@/app/rentals/[id]/payment/page")
    ).default;
    renderWithQuery(<PaymentPage />);

    await waitFor(() => {
      // 대여료 (totalAmount = 30,000)
      expect(screen.getByText(/30,000/)).toBeInTheDocument();
      // 보증금 (depositAmount = 10,000)
      expect(screen.getByText(/10,000/)).toBeInTheDocument();
    });
  });

  // TC-19: 결제하기 버튼이 존재해야 한다
  it("TC-19: 결제하기 버튼이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockApprovedRental,
      timestamp: new Date().toISOString(),
    });

    const PaymentPage = (
      await import("@/app/rentals/[id]/payment/page")
    ).default;
    renderWithQuery(<PaymentPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: /결제하기/ })
      ).toBeInTheDocument();
    });
  });

  // TC-20: APPROVED 아닌 상태에서 접근하면 접근 불가 메시지 표시
  it("TC-20: APPROVED 상태가 아닌 대여에 접근하면 접근 불가 메시지가 표시된다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: { ...mockApprovedRental, status: "REQUESTED" },
      timestamp: new Date().toISOString(),
    });

    const PaymentPage = (
      await import("@/app/rentals/[id]/payment/page")
    ).default;
    renderWithQuery(<PaymentPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/결제 가능한 상태가 아닙니다/)
      ).toBeInTheDocument();
    });
  });

  // TC-21: 결제 버튼 클릭 시 processPaymentApi 호출
  it("TC-21: 결제하기 버튼 클릭 시 POST /api/v1/rentals/{id}/payment가 호출된다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: mockApprovedRental,
      timestamp: new Date().toISOString(),
    });
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: {
        rentalId: 1003,
        paymentId: 5001,
        status: "PAID",
        externalPaymentId: "stub-key",
        paidAt: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    });

    const PaymentPage = (
      await import("@/app/rentals/[id]/payment/page")
    ).default;
    renderWithQuery(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /결제하기/ })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /결제하기/ }));

    await waitFor(() => {
      expect(sharedMockClient.post).toHaveBeenCalledWith(
        "/api/v1/rentals/1003/payment",
        expect.objectContaining({ amount: 40000 })
      );
    });
  });

  // TC-22: 로딩 상태가 표시되어야 한다
  it("TC-22: 데이터 로딩 중 로딩 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockReturnValue(new Promise(() => {}));

    const PaymentPage = (
      await import("@/app/rentals/[id]/payment/page")
    ).default;
    renderWithQuery(<PaymentPage />);

    expect(screen.getByText("불러오는 중...")).toBeInTheDocument();
  });
});
