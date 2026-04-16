import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { RentalSummary } from "@/lib/api/types";

// ========================================
// 내 대여 목록 페이지 테스트
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

const STUB_RENTALS: RentalSummary[] = [
  {
    rentalId: 1001,
    productId: 4,
    productName: "캠핑 텐트 4인용",
    productThumbnailUrl: "/images/stub/tent.jpg",
    status: "IN_USE",
    startDate: "2026-05-01",
    endDate: "2026-05-07",
    totalAmount: 120000,
    requestedAt: "2026-04-14T10:00:00+09:00",
  },
  {
    rentalId: 1002,
    productId: 1,
    productName: "소니 A7C II 카메라",
    productThumbnailUrl: null,
    status: "APPROVED",
    startDate: "2026-05-10",
    endDate: "2026-05-15",
    totalAmount: 175000,
    requestedAt: "2026-04-14T11:00:00+09:00",
  },
];

function buildPageResponse(rentals: RentalSummary[]) {
  return {
    success: true,
    data: {
      content: rentals,
      totalElements: rentals.length,
      totalPages: 1,
      size: 20,
      number: 0,
    },
    timestamp: new Date().toISOString(),
  };
}

describe("내 대여 목록 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("로딩 중에 스켈레톤이 표시되어야 한다", async () => {
    sharedMockClient.get.mockReturnValue(new Promise(() => {}));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    expect(screen.getByTestId("loading-state")).toBeInTheDocument();
  });

  it("대여 목록이 정상적으로 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildPageResponse(STUB_RENTALS));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("rental-list")).toBeInTheDocument();
    });

    expect(screen.getByText("캠핑 텐트 4인용")).toBeInTheDocument();
    expect(screen.getByText("소니 A7C II 카메라")).toBeInTheDocument();
  });

  it("빈 목록 시 빈 상태 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildPageResponse([]));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("empty-state")).toBeInTheDocument();
    });
  });

  it("API 오류 시 에러 상태가 표시되어야 한다", async () => {
    sharedMockClient.get.mockRejectedValue(new Error("API 오류"));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("error-state")).toBeInTheDocument();
    });
  });

  it("역할 탭이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildPageResponse([]));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    expect(screen.getByTestId("role-tabs")).toBeInTheDocument();
    expect(screen.getByTestId("tab-renter")).toBeInTheDocument();
    expect(screen.getByTestId("tab-lender")).toBeInTheDocument();
  });

  it("등록자 탭 클릭 시 LENDER role로 API가 호출되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildPageResponse([]));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("tab-lender")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("tab-lender"));

    await waitFor(() => {
      // 두 번 이상 호출 (탭 전환 시 재요청)
      expect(sharedMockClient.get).toHaveBeenCalledTimes(2);
    });
  });

  it("상태 필터가 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildPageResponse([]));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    expect(screen.getByTestId("status-filter")).toBeInTheDocument();
    expect(screen.getByTestId("filter-all")).toBeInTheDocument();
    expect(screen.getByTestId("filter-IN_USE")).toBeInTheDocument();
  });

  it("총 건수가 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue(buildPageResponse(STUB_RENTALS));

    const MyRentalsPage = (await import("@/app/my-rentals/page")).default;
    renderWithQuery(<MyRentalsPage />);

    await waitFor(() => {
      expect(screen.getByText("2")).toBeInTheDocument();
    });
  });
});
