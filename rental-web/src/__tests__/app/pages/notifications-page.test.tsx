import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { Notification, PaginatedResponse } from "@/lib/api/types";
import { STUB_NOTIFICATIONS } from "@/mocks/handlers/notification";

// ========================================
// 알림 페이지 테스트
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/notifications",
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
      mutations: { retry: false },
    },
  });
}

function renderWithQuery(ui: React.ReactElement) {
  const qc = createQueryClient();
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

// ========================================
// 테스트
// ========================================

describe("알림 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("'알림' 제목이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: {
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      } as PaginatedResponse<Notification>,
      timestamp: new Date().toISOString(),
    });

    const { default: NotificationsPage } = await import("@/app/(auth)/notifications/page");
    renderWithQuery(<NotificationsPage />);

    expect(screen.getByText("알림")).toBeInTheDocument();
  });

  it("알림 목록이 렌더링되어야 한다", async () => {
    const notifications = STUB_NOTIFICATIONS.map((n) => ({ ...n }));

    sharedMockClient.get.mockImplementation(async (endpoint: string) => {
      if (endpoint.includes("/notifications")) {
        return {
          success: true,
          data: {
            content: notifications,
            page: 0,
            size: 10,
            totalElements: notifications.length,
            totalPages: 1,
            hasNext: false,
          } as PaginatedResponse<Notification>,
          timestamp: new Date().toISOString(),
        };
      }
      return { success: true, data: {}, timestamp: "" };
    });

    sharedMockClient.patch.mockResolvedValue({
      success: true,
      data: null,
      timestamp: new Date().toISOString(),
    });

    const { default: NotificationsPage } = await import("@/app/(auth)/notifications/page");
    renderWithQuery(<NotificationsPage />);

    await waitFor(() => {
      expect(screen.getByText("대여 요청이 들어왔습니다")).toBeInTheDocument();
    });
  });
});
