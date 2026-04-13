import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { MyPageInfo } from "@/lib/api/types";
import { STUB_MYPAGE_INFO } from "@/mocks/handlers/mypage";

// ========================================
// 프로필 수정 페이지 테스트
// ========================================

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/mypage/edit",
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
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

describe("프로필 수정 페이지", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPush.mockReset();
  });

  it("데이터 로딩 중에 로딩 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockReturnValue(new Promise(() => {}));

    const ProfileEditPage = (await import("@/app/(auth)/mypage/edit/page"))
      .default;
    renderWithQuery(<ProfileEditPage />);

    expect(screen.getByText("불러오는 중...")).toBeInTheDocument();
  });

  it("프로필 수정 폼이 렌더링되어야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: STUB_MYPAGE_INFO as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    const ProfileEditPage = (await import("@/app/(auth)/mypage/edit/page"))
      .default;
    renderWithQuery(<ProfileEditPage />);

    await waitFor(() => {
      expect(screen.getByText("프로필 수정")).toBeInTheDocument();
    });

    expect(screen.getByText("기본 정보 수정")).toBeInTheDocument();
  });

  it("기존 데이터가 폼에 채워져야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: STUB_MYPAGE_INFO as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    const ProfileEditPage = (await import("@/app/(auth)/mypage/edit/page"))
      .default;
    renderWithQuery(<ProfileEditPage />);

    await waitFor(() => {
      const nameInput = screen.getByDisplayValue("김대여");
      expect(nameInput).toBeInTheDocument();
    });
  });

  it("이메일 필드가 비활성화(readonly)여야 한다", async () => {
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: STUB_MYPAGE_INFO as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    const ProfileEditPage = (await import("@/app/(auth)/mypage/edit/page"))
      .default;
    renderWithQuery(<ProfileEditPage />);

    await waitFor(() => {
      const emailInput = screen.getByDisplayValue(STUB_MYPAGE_INFO.email);
      expect(emailInput).toBeDisabled();
    });
  });

  it("프로필 수정 성공 시 /mypage로 이동해야 한다", async () => {
    const user = userEvent.setup();

    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: STUB_MYPAGE_INFO as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    const updatedInfo = { ...STUB_MYPAGE_INFO, name: "수정된이름" };
    sharedMockClient.patch.mockResolvedValue({
      success: true,
      data: updatedInfo as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    const ProfileEditPage = (await import("@/app/(auth)/mypage/edit/page"))
      .default;
    renderWithQuery(<ProfileEditPage />);

    await waitFor(() => {
      expect(screen.getByText("프로필 수정")).toBeInTheDocument();
    });

    // 이름 수정
    const nameInput = screen.getByDisplayValue("김대여");
    await user.clear(nameInput);
    await user.type(nameInput, "수정된이름");

    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(
      () => {
        expect(mockPush).toHaveBeenCalledWith("/mypage");
      },
      { timeout: 3000 }
    );
  });

  it("프로필 수정 실패 시 에러 메시지가 표시되어야 한다", async () => {
    const user = userEvent.setup();

    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: STUB_MYPAGE_INFO as MyPageInfo,
      timestamp: new Date().toISOString(),
    });

    sharedMockClient.patch.mockRejectedValue(new Error("수정 실패"));

    const ProfileEditPage = (await import("@/app/(auth)/mypage/edit/page"))
      .default;
    renderWithQuery(<ProfileEditPage />);

    await waitFor(() => {
      expect(screen.getByText("프로필 수정")).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(
      () => {
        expect(screen.getByText("수정 실패")).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });

  it("API 오류 시 에러 메시지가 표시되어야 한다", async () => {
    sharedMockClient.get.mockRejectedValue(new Error("데이터 로드 실패"));

    const ProfileEditPage = (await import("@/app/(auth)/mypage/edit/page"))
      .default;
    renderWithQuery(<ProfileEditPage />);

    await waitFor(() => {
      expect(
        screen.getByText("정보를 불러오지 못했습니다. 다시 시도해주세요.")
      ).toBeInTheDocument();
    });
  });
});
