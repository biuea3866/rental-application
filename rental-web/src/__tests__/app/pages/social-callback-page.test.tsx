import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import SocialCallbackPage from "@/app/(auth)/login/social/callback/page";
import { useAuthStore } from "@/stores/auth-store";
import { clearTokens } from "@/lib/auth/token";

// ========================================
// 소셜 로그인 콜백 페이지 테스트 (성공 케이스)
// useSearchParams: code=valid-code, state=KAKAO
// ========================================

const mockPush = vi.fn();
const mockReplace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/login/social/callback",
  // code와 state가 모두 있는 정상 케이스
  useSearchParams: () => new URLSearchParams({ code: "valid-code", state: "KAKAO" }),
}));

describe("소셜 로그인 콜백 페이지 - 성공 케이스", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearTokens();
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  });

  it("로딩 스피너가 렌더링되어야 한다", async () => {
    render(<SocialCallbackPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("status", { name: "로그인 처리 중" })
      ).toBeInTheDocument();
    });
  });

  it("'로그인 처리 중...' 텍스트가 렌더링되어야 한다", async () => {
    render(<SocialCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText("로그인 처리 중...")).toBeInTheDocument();
    });
  });

  it("유효한 code와 state로 소셜 로그인 성공 시 홈으로 이동해야 한다", async () => {
    render(<SocialCallbackPage />);

    await waitFor(
      () => {
        expect(mockReplace).toHaveBeenCalledWith("/");
      },
      { timeout: 3000 }
    );
  });
});
