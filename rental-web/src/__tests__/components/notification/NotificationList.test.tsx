import { render, screen, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, beforeEach, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NotificationList } from "@/components/notification/NotificationList";
import { resetNotificationHandlerState, STUB_NOTIFICATIONS } from "@/mocks/handlers/notification";
import type { ApiClient, ApiResponse } from "@/lib/api/client";
import type { Notification, PaginatedResponse } from "@/lib/api/types";

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
  usePathname: () => "/notifications",
}));

// ========================================
// API 클라이언트 mock
// ========================================

// 전역 알림 상태 (mutation 테스트 지원)
let mockNotifications = STUB_NOTIFICATIONS.map((n) => ({ ...n }));

const mockApiClient: ApiClient = {
  get: vi.fn(async (endpoint: string) => {
    if (endpoint.includes("unread-count")) {
      const count = mockNotifications.filter((n) => !n.isRead).length;
      return { success: true, data: { count }, timestamp: "" } as ApiResponse<{ count: number }>;
    }
    if (endpoint.includes("/notifications")) {
      const url = new URL(`http://localhost${endpoint}`);
      const page = parseInt(url.searchParams.get("page") || "0", 10);
      const size = parseInt(url.searchParams.get("size") || "10", 10);
      const start = page * size;
      const end = start + size;
      const pageContent = mockNotifications.slice(start, end);
      return {
        success: true,
        data: {
          content: pageContent,
          page,
          size,
          totalElements: mockNotifications.length,
          totalPages: Math.ceil(mockNotifications.length / size),
          hasNext: end < mockNotifications.length,
        } as PaginatedResponse<Notification>,
        timestamp: "",
      } as ApiResponse<PaginatedResponse<Notification>>;
    }
    return { success: true, data: {}, timestamp: "" } as ApiResponse<unknown>;
  }),
  post: vi.fn(async () => ({ success: true, data: {}, timestamp: "" } as ApiResponse<unknown>)),
  put: vi.fn(async () => ({ success: true, data: {}, timestamp: "" } as ApiResponse<unknown>)),
  patch: vi.fn(async (endpoint: string) => {
    if (endpoint.includes("read-all")) {
      mockNotifications = mockNotifications.map((n) => ({ ...n, isRead: true }));
      return { success: true, data: null, timestamp: "" } as ApiResponse<null>;
    }
    // read by id: /api/v1/notifications/:id/read
    const idMatch = endpoint.match(/\/notifications\/([^/]+)\/read/);
    if (idMatch) {
      const id = idMatch[1];
      mockNotifications = mockNotifications.map((n) =>
        n.id === id ? { ...n, isRead: true } : n
      );
      return { success: true, data: {}, timestamp: "" } as ApiResponse<unknown>;
    }
    return { success: true, data: {}, timestamp: "" } as ApiResponse<unknown>;
  }),
  delete: vi.fn(async () => ({ success: true, data: {}, timestamp: "" } as ApiResponse<unknown>)),
};

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
// NotificationList 테스트
// ========================================

describe("NotificationList", () => {
  beforeEach(() => {
    resetNotificationHandlerState();
    // mock 상태 초기화
    mockNotifications = STUB_NOTIFICATIONS.map((n) => ({ ...n }));
    vi.clearAllMocks();
    // mock 재설정
    (mockApiClient.get as ReturnType<typeof vi.fn>).mockImplementation(async (endpoint: string) => {
      if (endpoint.includes("/notifications")) {
        const url = new URL(`http://localhost${endpoint}`);
        const page = parseInt(url.searchParams.get("page") || "0", 10);
        const size = parseInt(url.searchParams.get("size") || "10", 10);
        const start = page * size;
        const end = start + size;
        const pageContent = mockNotifications.slice(start, end);
        return {
          success: true,
          data: {
            content: pageContent,
            page,
            size,
            totalElements: mockNotifications.length,
            totalPages: Math.ceil(mockNotifications.length / size),
            hasNext: end < mockNotifications.length,
          },
          timestamp: "",
        };
      }
      return { success: true, data: {}, timestamp: "" };
    });
    (mockApiClient.patch as ReturnType<typeof vi.fn>).mockImplementation(async (endpoint: string) => {
      if (endpoint.includes("read-all")) {
        mockNotifications = mockNotifications.map((n) => ({ ...n, isRead: true }));
        return { success: true, data: null, timestamp: "" };
      }
      const idMatch = endpoint.match(/\/notifications\/([^/]+)\/read/);
      if (idMatch) {
        const id = idMatch[1];
        mockNotifications = mockNotifications.map((n) =>
          n.id === id ? { ...n, isRead: true } : n
        );
        return { success: true, data: {}, timestamp: "" };
      }
      return { success: true, data: {}, timestamp: "" };
    });
  });

  it("알림 목록이 렌더링되어야 한다", async () => {
    renderWithQuery(<NotificationList />);

    // 로딩 상태 확인
    expect(screen.getByText("알림을 불러오는 중...")).toBeInTheDocument();

    // 알림 목록 렌더링 확인
    await waitFor(() => {
      expect(
        screen.getByText("대여 요청이 들어왔습니다")
      ).toBeInTheDocument();
    });
  });

  it("읽지 않은 알림 개수가 표시되어야 한다", async () => {
    renderWithQuery(<NotificationList />);

    await waitFor(() => {
      expect(screen.getByText(/읽지 않은 알림/)).toBeInTheDocument();
    });
  });

  it("읽지 않은 알림이 있으면 '전체 읽음' 버튼이 표시되어야 한다", async () => {
    renderWithQuery(<NotificationList />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "전체 읽음" })).toBeInTheDocument();
    });
  });

  it("'전체 읽음' 버튼 클릭 시 모든 알림이 읽음 처리되어야 한다", async () => {
    const user = userEvent.setup();
    renderWithQuery(<NotificationList />);

    // 알림 목록 렌더링 대기
    await waitFor(() => {
      expect(
        screen.getByText("대여 요청이 들어왔습니다")
      ).toBeInTheDocument();
    });

    const readAllButton = screen.getByRole("button", { name: "전체 읽음" });
    await act(async () => {
      await user.click(readAllButton);
    });

    // 전체 읽음 처리 후 버튼이 사라져야 함
    await waitFor(() => {
      expect(
        screen.queryByRole("button", { name: "전체 읽음" })
      ).not.toBeInTheDocument();
    });
  });

  it("개별 읽지 않은 알림 클릭 시 읽음 처리되어야 한다", async () => {
    const user = userEvent.setup();
    renderWithQuery(<NotificationList />);

    await waitFor(() => {
      expect(
        screen.getByText("대여 요청이 들어왔습니다")
      ).toBeInTheDocument();
    });

    // 읽지 않은 알림 클릭
    const unreadItem = screen.getByText("대여 요청이 들어왔습니다").closest("li");
    expect(unreadItem).not.toBeNull();

    await act(async () => {
      await user.click(unreadItem!);
    });

    // 읽음 처리 후 unread count 감소 확인
    await waitFor(() => {
      // 원래 2개 unread였으므로 1개가 남음
      expect(screen.getByText(/읽지 않은 알림 1개/)).toBeInTheDocument();
    });
  });

  it("알림 목록 역할(role=list)이 렌더링되어야 한다", async () => {
    renderWithQuery(<NotificationList />);

    await waitFor(() => {
      expect(screen.getByRole("list", { name: "알림 목록" })).toBeInTheDocument();
    });
  });

  it("API 오류 시 에러 메시지가 표시되어야 한다", async () => {
    (mockApiClient.get as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error("알림 조회 실패")
    );

    renderWithQuery(<NotificationList />);

    await waitFor(() => {
      expect(screen.getByText("알림을 불러오지 못했습니다.")).toBeInTheDocument();
    });
  });

  it("알림이 없을 때 '알림이 없습니다.' 메시지가 표시되어야 한다", async () => {
    (mockApiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      success: true,
      data: {
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      },
      timestamp: "",
    });

    renderWithQuery(<NotificationList />);

    await waitFor(() => {
      expect(screen.getByText("알림이 없습니다.")).toBeInTheDocument();
    });
  });

  it("다음 페이지가 없으면 '전체 읽음' 버튼이 없어야 한다 (모두 읽음 상태)", async () => {
    const allReadNotifications = STUB_NOTIFICATIONS.map((n) => ({ ...n, isRead: true }));

    (mockApiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      success: true,
      data: {
        content: allReadNotifications,
        page: 0,
        size: 10,
        totalElements: allReadNotifications.length,
        totalPages: 1,
        hasNext: false,
      },
      timestamp: "",
    });

    renderWithQuery(<NotificationList />);

    await waitFor(() => {
      expect(screen.getByText("모두 읽었습니다")).toBeInTheDocument();
    });

    expect(screen.queryByRole("button", { name: "전체 읽음" })).not.toBeInTheDocument();
  });
});
