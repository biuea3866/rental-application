import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { useAuthStore } from "@/stores/auth-store";
import { clearTokens } from "@/lib/auth/token";

// ========================================
// 소셜 로그인 콜백 페이지 테스트 (에러 케이스)
// - error 파라미터가 있을 때
// - code/state가 없을 때
// ========================================

const mockPush = vi.fn();
const mockReplace = vi.fn();

// error 파라미터가 있는 케이스
vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/login/social/callback",
  useSearchParams: () => new URLSearchParams({ error: "access_denied" }),
}));

describe("소셜 로그인 콜백 페이지 - 에러 케이스", () => {
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

  it("error 파라미터가 있으면 로그인 에러 페이지로 이동해야 한다", async () => {
    const SocialCallbackPage = (
      await import("@/app/(auth)/login/social/callback/page")
    ).default;

    render(<SocialCallbackPage />);

    await waitFor(
      () => {
        expect(mockReplace).toHaveBeenCalledWith(
          "/login?error=social_login_failed"
        );
      },
      { timeout: 3000 }
    );
  });

  it("error 파라미터가 있을 때 로딩 스피너가 렌더링되어야 한다", async () => {
    const SocialCallbackPage = (
      await import("@/app/(auth)/login/social/callback/page")
    ).default;

    render(<SocialCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText("로그인 처리 중...")).toBeInTheDocument();
    });
  });
});
