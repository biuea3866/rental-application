import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { RentalDetail } from "@/lib/api/types";

// ========================================
// 대여 상세 페이지 테스트
// RC-FE-215
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/rentals/1001",
}));

// React.use() mock
vi.mock("react", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react")>();
  return {
    ...actual,
    use: (promise: Promise<unknown>) => {
      // 테스트용: Promise를 동기 해제로 처리
      if (promise instanceof Promise) {
        return { id: "1001" };
      }
      return promise;
    },
  };
});

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

const STUB_RENTAL_DETAIL: RentalDetail = {
  rentalId: 1001,
  product: {
    productId: 4,
    name: "캠핑 텐트 4인용",
    thumbnailUrl: "/images/stub/tent.jpg",
    category: "SPORTS",
  },
  renter: { userId: 3, name: "이빌림" },
  lender: { userId: 1, name: "김대여" },
  status: "IN_USE",
  startDate: "2026-05-01",
  endDate: "2026-05-07",
  totalAmount: 120000,
  depositAmount: 200000,
  deliveryInfo: {
    recipientName: "이빌림",
    recipientPhone: "010-3456-7890",
    addressLine1: "서울특별시 강남구 테헤란로 123",
    addressLine2: "101호",
    zipCode: "06234",
  },
  payment: {
    paymentId: 5001,
    amount: 120000,
    paymentMethod: "CARD",
    status: "COMPLETED",
    paidAt: "2026-04-14T12:00:00+09:00",
  },
  timeline: [
    { status: "REQUESTED" as const, occurredAt: "2026-04-14T10:00:00+09:00" },
    { status: "APPROVED" as const, occurredAt: "2026-04-14T11:00:00+09:00" },
    { status: "PAID" as const, occurredAt: "2026-04-14T12:00:00+09:00" },
    { status: "IN_USE" as const, occurredAt: "2026-04-15T09:00:00+09:00" },
  ],
  requestedAt: "2026-04-14T10:00:00+09:00",
  approvedAt: "2026-04-14T11:00:00+09:00",
  paidAt: "2026-04-14T12:00:00+09:00",
  startedAt: "2026-04-15T09:00:00+09:00",
};

function buildDetailResponse(rental: RentalDetail) {
  return {
    success: true,
    data: rental,
    timestamp: new Date().toISOString(),
  };
}

describe("대여 상세 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("로딩 중 스켈레톤이 표시되어야 한다", async () => {
    sharedMockClient.get.mockReturnValue(new Promise(() => {}));

    const RentalDetailPage = (await import("@/app/rentals/[id]/page")).default;
    renderWithQuery(<RentalDetailPage params={Promise.resolve({ id: "1001" })} />);

    await waitFor(() => {
      expect(screen.getByTestId("loading-state")).toBeInTheDocument();
    });
  });

  it("상품명이 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildDetailResponse(STUB_RENTAL_DETAIL));

    const RentalDetailPage = (await import("@/app/rentals/[id]/page")).default;
    renderWithQuery(<RentalDetailPage params={Promise.resolve({ id: "1001" })} />);

    await waitFor(() => {
      expect(screen.getByText("캠핑 텐트 4인용")).toBeInTheDocument();
    });
  });

  it("상태 타임라인 섹션이 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildDetailResponse(STUB_RENTAL_DETAIL));

    const RentalDetailPage = (await import("@/app/rentals/[id]/page")).default;
    renderWithQuery(<RentalDetailPage params={Promise.resolve({ id: "1001" })} />);

    await waitFor(() => {
      expect(screen.getByTestId("timeline-section")).toBeInTheDocument();
    });
  });

  it("결제 정보가 PAID 이후에 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildDetailResponse(STUB_RENTAL_DETAIL));

    const RentalDetailPage = (await import("@/app/rentals/[id]/page")).default;
    renderWithQuery(<RentalDetailPage params={Promise.resolve({ id: "1001" })} />);

    await waitFor(() => {
      expect(screen.getByTestId("payment-section")).toBeInTheDocument();
    });
  });

  it("결제 정보가 없을 때 결제 섹션이 표시되지 않아야 한다", async () => {
    const requestedRental: RentalDetail = {
      ...STUB_RENTAL_DETAIL,
      status: "REQUESTED",
      payment: undefined,
    };
    sharedMockClient.get.mockResolvedValue(buildDetailResponse(requestedRental));

    const RentalDetailPage = (await import("@/app/rentals/[id]/page")).default;
    renderWithQuery(<RentalDetailPage params={Promise.resolve({ id: "1001" })} />);

    await waitFor(() => {
      expect(screen.getByTestId("product-section")).toBeInTheDocument();
    });

    expect(screen.queryByTestId("payment-section")).not.toBeInTheDocument();
  });

  it("API 오류 시 에러 상태가 표시되어야 한다", async () => {
    sharedMockClient.get.mockRejectedValue(new Error("API 오류"));

    const RentalDetailPage = (await import("@/app/rentals/[id]/page")).default;
    renderWithQuery(<RentalDetailPage params={Promise.resolve({ id: "9999" })} />);

    await waitFor(() => {
      expect(screen.getByTestId("error-state")).toBeInTheDocument();
    });
  });

  it("대여 기간 정보가 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildDetailResponse(STUB_RENTAL_DETAIL));

    const RentalDetailPage = (await import("@/app/rentals/[id]/page")).default;
    renderWithQuery(<RentalDetailPage params={Promise.resolve({ id: "1001" })} />);

    await waitFor(() => {
      expect(screen.getByTestId("rental-info-section")).toBeInTheDocument();
    });

    expect(screen.getByText("이빌림")).toBeInTheDocument();
    expect(screen.getByText("김대여")).toBeInTheDocument();
  });
});
