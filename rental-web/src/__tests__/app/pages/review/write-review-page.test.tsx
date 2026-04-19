import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// ========================================
// 리뷰 작성 페이지 테스트
// RC-FE-317
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/my-rentals/1001/review",
  useParams: () => ({ id: "1001" }),
  useSearchParams: () => ({
    get: (key: string) => (key === "productId" ? "42" : null),
  }),
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

describe("리뷰 작성 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("리뷰 작성 폼이 렌더링되어야 한다", async () => {
    const WriteReviewPage = (
      await import("@/app/(auth)/my-rentals/[id]/review/page")
    ).default;
    renderWithQuery(<WriteReviewPage />);

    expect(screen.getByTestId("review-form")).toBeInTheDocument();
  });

  it("페이지 제목이 표시되어야 한다", async () => {
    const WriteReviewPage = (
      await import("@/app/(auth)/my-rentals/[id]/review/page")
    ).default;
    renderWithQuery(<WriteReviewPage />);

    expect(screen.getByText("리뷰 작성")).toBeInTheDocument();
  });

  it("대여 번호가 안내 문구에 표시되어야 한다", async () => {
    const WriteReviewPage = (
      await import("@/app/(auth)/my-rentals/[id]/review/page")
    ).default;
    renderWithQuery(<WriteReviewPage />);

    expect(screen.getByText(/대여 #1001/)).toBeInTheDocument();
  });

  it("리뷰 제출 성공 시 my-rentals로 이동해야 한다", async () => {
    const push = vi.fn();
    vi.mocked(
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      (await import("next/navigation")).useRouter
    )().push = push;

    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: {
        reviewId: 10,
        renterId: 11,
        rentalId: 1001,
        productId: 42,
        rating: 5,
        content: "정말 만족스러운 대여 경험이었습니다!",
        createdAt: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    });

    const WriteReviewPage = (
      await import("@/app/(auth)/my-rentals/[id]/review/page")
    ).default;
    renderWithQuery(<WriteReviewPage />);

    fireEvent.click(screen.getByTestId("star-5"));
    fireEvent.change(screen.getByTestId("review-content-input"), {
      target: { value: "정말 만족스러운 대여 경험이었습니다!" },
    });

    await waitFor(() => {
      expect(screen.getByTestId("review-submit-button")).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId("review-submit-button"));

    await waitFor(() => {
      expect(sharedMockClient.post).toHaveBeenCalled();
    });
  });
});
