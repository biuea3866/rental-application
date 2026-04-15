import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ProductSummary } from "@/lib/api/types";
import { STUB_PRODUCT_SUMMARIES } from "@/mocks/products";

// ========================================
// 등록자 상품 관리 페이지 테스트
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/lender/products",
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

// ========================================
// 테스트 헬퍼
// ========================================

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0, gcTime: 0 },
    },
  });
}

function renderWithQuery(ui: React.ReactElement) {
  const qc = createQueryClient();
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

// ========================================
// 테스트 데이터
// ========================================

// userId=1 소유 상품 (STUB_PRODUCTS.userId === 1)
const myProducts = STUB_PRODUCT_SUMMARIES.filter((_, i) => i % 2 === 0);

interface SpringPageResult {
  content: ProductSummary[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

function makeProductsResponse(products: ProductSummary[]): SpringPageResult {
  return {
    content: products,
    number: 0,
    size: 20,
    totalElements: products.length,
    totalPages: products.length > 0 ? 1 : 0,
    last: true,
  };
}

// ========================================
// 테스트
// ========================================

describe("등록자 내 상품 관리 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("데이터 로딩 중에 로딩 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockReturnValue(new Promise(() => {}));

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    expect(screen.getByText("불러오는 중...")).toBeInTheDocument();
  });

  it("'내 등록 상품' 제목이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: makeProductsResponse(myProducts),
      timestamp: new Date().toISOString(),
    });

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    await waitFor(() => {
      expect(screen.getByText("내 등록 상품")).toBeInTheDocument();
    });
  });

  it("'상품 등록' 버튼이 /lender/products/new로 연결되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: makeProductsResponse(myProducts),
      timestamp: new Date().toISOString(),
    });

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    await waitFor(() => {
      const registerLink = screen.getByRole("link", { name: "상품 등록" });
      expect(registerLink).toHaveAttribute("href", "/lender/products/new");
    });
  });

  it("상품 목록이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: makeProductsResponse(myProducts),
      timestamp: new Date().toISOString(),
    });

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    await waitFor(() => {
      expect(screen.getByText("소니 A7C II 미러리스 카메라")).toBeInTheDocument();
    });
  });

  it("요약 통계가 표시되어야 한다 (전체/대여가능/대여중)", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: makeProductsResponse(myProducts),
      timestamp: new Date().toISOString(),
    });

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    await waitFor(() => {
      // 요약 통계 영역이 있어야 한다
      expect(screen.getByText("전체 상품")).toBeInTheDocument();
      // "대여 가능"은 통계 카드 + 상품 뱃지에 여러 번 나올 수 있으므로 getAllByText 사용
      expect(screen.getAllByText("대여 가능").length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText("검토 중")).toBeInTheDocument();
    });
  });

  it("상품이 없을 때 빈 상태 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: makeProductsResponse([]),
      timestamp: new Date().toISOString(),
    });

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    await waitFor(() => {
      expect(screen.getByText("등록된 상품이 없습니다.")).toBeInTheDocument();
      expect(screen.getByText("첫 상품 등록하기")).toBeInTheDocument();
    });
  });

  it("API 오류 시 에러 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockRejectedValue(new Error("API 오류"));

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    await waitFor(() => {
      expect(
        screen.getByText("상품을 불러오지 못했습니다. 다시 시도해주세요.")
      ).toBeInTheDocument();
    });
  });

  it("각 상품 카드에 수정 링크가 있어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: makeProductsResponse([myProducts[0]]),
      timestamp: new Date().toISOString(),
    });

    const LenderProductsPage = (
      await import("@/app/(lender)/lender/products/page")
    ).default;
    renderWithQuery(<LenderProductsPage />);

    await waitFor(() => {
      const editLink = screen.getByRole("link", { name: "수정" });
      expect(editLink).toHaveAttribute(
        "href",
        `/lender/products/${myProducts[0].id}/edit`
      );
    });
  });
});
