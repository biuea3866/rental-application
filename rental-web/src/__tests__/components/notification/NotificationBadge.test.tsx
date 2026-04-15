import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, beforeEach, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NotificationBadge } from "@/components/notification/NotificationBadge";
import { useNotificationStore } from "@/stores/notification-store";
import { resetNotificationHandlerState } from "@/mocks/handlers/notification";
import type { ApiClient, ApiResponse } from "@/lib/api/client";

// ========================================
// Next.js 라우터 mock
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/",
}));

// ========================================
// API 클라이언트 mock
// ========================================

let mockUnreadCount = 2;

const mockApiClient = {
  get: vi.fn(async () =>
    ({ success: true, data: { count: mockUnreadCount }, timestamp: "" })
  ),
  post: vi.fn(async () => ({ success: true, data: {}, timestamp: "" })),
  put: vi.fn(async () => ({ success: true, data: {}, timestamp: "" })),
  patch: vi.fn(async () => ({ success: true, data: {}, timestamp: "" })),
  delete: vi.fn(async () => ({ success: true, data: {}, timestamp: "" })),
} as unknown as ApiClient;

vi.mock("@/lib/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/client")>();
  return {
    ...actual,
    getApiClient: () => mockApiClient,
  };
});

// ========================================
// 테스트 유틸
// ========================================

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        staleTime: 0,
        gcTime: 0,
        // 테스트에서는 자동 refetch 비활성
        refetchInterval: false,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

function renderWithQuery(ui: React.ReactElement) {
  const queryClient = createTestQueryClient();
  return {
    ...render(
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    ),
    queryClient,
  };
}

// ========================================
// NotificationBadge 테스트
// ========================================

describe("NotificationBadge", () => {
  beforeEach(() => {
    resetNotificationHandlerState();
    mockUnreadCount = 2;
    useNotificationStore.setState({ unreadCount: 0 });
    vi.clearAllMocks();
    (mockApiClient.get as ReturnType<typeof vi.fn>).mockImplementation(async () =>
      ({ success: true, data: { count: mockUnreadCount }, timestamp: "" })
    );
  });

  it("알림 링크가 /notifications로 연결되어야 한다", () => {
    renderWithQuery(<NotificationBadge />);

    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("href", "/notifications");
  });

  it("미읽음 수가 0이면 배지가 표시되지 않아야 한다", () => {
    useNotificationStore.setState({ unreadCount: 0 });
    renderWithQuery(<NotificationBadge />);

    // 배지 숫자 없음
    expect(screen.queryByText(/\d+/)).not.toBeInTheDocument();
  });

  it("미읽음 수가 있으면 배지에 숫자가 표시되어야 한다", async () => {
    useNotificationStore.setState({ unreadCount: 2 });
    renderWithQuery(<NotificationBadge />);

    expect(screen.getByText("2")).toBeInTheDocument();
  });

  it("미읽음 수가 99 초과이면 99+로 표시되어야 한다", () => {
    useNotificationStore.setState({ unreadCount: 100 });
    renderWithQuery(<NotificationBadge />);

    expect(screen.getByText("99+")).toBeInTheDocument();
  });

  it("API에서 미읽음 수를 불러와 스토어에 반영해야 한다", async () => {
    // 초기 stub은 2개 unread (notif-001, notif-002)
    renderWithQuery(<NotificationBadge />);

    await waitFor(() => {
      const store = useNotificationStore.getState();
      expect(store.unreadCount).toBe(2);
    });

    // 배지 표시 확인
    await waitFor(() => {
      expect(screen.getByText("2")).toBeInTheDocument();
    });
  });

  it("링크에 접근성 aria-label이 있어야 한다", () => {
    renderWithQuery(<NotificationBadge />);

    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("aria-label");
  });
});
