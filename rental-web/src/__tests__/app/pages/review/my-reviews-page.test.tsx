import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReviewListResponse } from "@/lib/api/types";
import { STUB_REVIEWS } from "@/mocks/handlers/review";

// ========================================
// 내 리뷰 목록 페이지 테스트
// RC-FE-319
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/mypage/reviews",
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
    defaultOptions: { queries: { retry: false, staleTime: 0, gcTime: 0 } },
  });
}

function renderWithQuery(ui: React.ReactElement) {
  const qc = createQueryClient();
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe("내 리뷰 목록 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("로딩 중 스켈레톤이 표시되어야 한다", async () => {
    sharedMockClient.get.mockReturnValue(new Promise(() => {}));

    const MyReviewsPage = (
      await import("@/app/(auth)/mypage/reviews/page")
    ).default;
    renderWithQuery(<MyReviewsPage />);

    expect(screen.getByTestId("loading-state")).toBeInTheDocument();
  });

  it("리뷰 목록이 정상적으로 렌더링되어야 한다", async () => {
    const stubData: ReviewListResponse = {
      content: STUB_REVIEWS,
      totalElements: STUB_REVIEWS.length,
      totalPages: 1,
    };

    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: stubData,
      timestamp: new Date().toISOString(),
    });

    const MyReviewsPage = (
      await import("@/app/(auth)/mypage/reviews/page")
    ).default;
    renderWithQuery(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("review-list")).toBeInTheDocument();
    });

    expect(screen.getByTestId(`review-card-${STUB_REVIEWS[0].reviewId}`)).toBeInTheDocument();
  });

  it("리뷰가 없을 때 빈 상태가 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: { content: [], totalElements: 0, totalPages: 0 },
      timestamp: new Date().toISOString(),
    });

    const MyReviewsPage = (
      await import("@/app/(auth)/mypage/reviews/page")
    ).default;
    renderWithQuery(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("empty-state")).toBeInTheDocument();
    });
  });

  it("API 에러 시 에러 상태가 표시되어야 한다", async () => {
    sharedMockClient.get.mockRejectedValue(new Error("API 오류"));

    const MyReviewsPage = (
      await import("@/app/(auth)/mypage/reviews/page")
    ).default;
    renderWithQuery(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("error-state")).toBeInTheDocument();
    });
  });
});
