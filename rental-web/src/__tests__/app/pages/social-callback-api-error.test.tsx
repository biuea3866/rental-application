import { render, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { useAuthStore } from "@/stores/auth-store";
import { clearTokens } from "@/lib/auth/token";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";

// ========================================
// 소셜 로그인 콜백 페이지 테스트 (API 오류 케이스)
// - code, state는 있지만 API 호출 실패
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
  useSearchParams: () =>
    new URLSearchParams({ code: "invalid-code", state: "NAVER" }),
}));

describe("소셜 로그인 콜백 페이지 - API 오류 케이스", () => {
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

  it("API 호출 실패 시 로그인 에러 페이지로 이동해야 한다", async () => {
    // 소셜 로그인 API를 실패하게 설정
    server.use(
      http.post("http://localhost:8080/api/v1/auth/social-login", () => {
        return HttpResponse.json(
          { code: "INVALID_CODE", message: "유효하지 않은 코드" },
          { status: 400 }
        );
      })
    );

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
});
