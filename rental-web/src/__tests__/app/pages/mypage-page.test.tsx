import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { MyPageInfo } from "@/lib/api/types";
import { STUB_MYPAGE_INFO } from "@/mocks/handlers/mypage";

// ========================================
// 마이페이지 테스트
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/mypage",
}));

// 공유 mock 클라이언트 (싱글턴처럼 사용)
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
// 테스트
// ========================================

describe("마이페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("데이터 로딩 중에 로딩 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockReturnValue(new Promise(() => {}));

    const MyPage = (await import("@/app/(auth)/mypage/page")).default;
    renderWithQuery(<MyPage />);

    expect(screen.getByText("불러오는 중...")).toBeInTheDocument();
  });

  it("LENDER 유저의 마이페이지를 정상적으로 렌더링해야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: STUB_MYPAGE_INFO as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    const MyPage = (await import("@/app/(auth)/mypage/page")).default;
    renderWithQuery(<MyPage />);

    await waitFor(() => {
      expect(screen.getByText("마이페이지")).toBeInTheDocument();
    });

    expect(screen.getByText("김대여")).toBeInTheDocument();
  });

  it("LENDER 역할 시 LenderProfileSection이 표시되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: { ...STUB_MYPAGE_INFO, role: "LENDER" } as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    const MyPage = (await import("@/app/(auth)/mypage/page")).default;
    renderWithQuery(<MyPage />);

    await waitFor(() => {
      expect(screen.getByText("등록자 프로필")).toBeInTheDocument();
    });
  });

  it("RENTER 역할 시 RenterProfileSection이 표시되어야 한다", async () => {
    const renterInfo: MyPageInfo = {
      id: "user-renter-001",
      email: "renter@rental.com",
      name: "이빌림",
      phone: "010-3456-7890",
      role: "RENTER",
      createdAt: "2026-01-20T11:00:00Z",
      renterProfile: {
        deliveryAddress: "서울 강남구",
        preferredCategories: ["ELECTRONICS"],
      },
    };

    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: renterInfo,
      timestamp: new Date().toISOString(),
    });

    const MyPage = (await import("@/app/(auth)/mypage/page")).default;
    renderWithQuery(<MyPage />);

    await waitFor(() => {
      expect(screen.getByText("대여자 프로필")).toBeInTheDocument();
    });
  });

  it("API 오류 시 에러 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockRejectedValue(new Error("API 오류"));

    const MyPage = (await import("@/app/(auth)/mypage/page")).default;
    renderWithQuery(<MyPage />);

    await waitFor(() => {
      expect(
        screen.getByText("정보를 불러오지 못했습니다. 다시 시도해주세요.")
      ).toBeInTheDocument();
    });
  });
});
