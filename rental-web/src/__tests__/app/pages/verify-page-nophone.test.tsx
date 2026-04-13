import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";

// ========================================
// 인증코드 페이지 - phone 없는 케이스 테스트
// ========================================

const mockPush = vi.fn();
const mockReplace = vi.fn();

// phone 없이 접근하는 케이스
vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/signup/verify",
  useSearchParams: () => new URLSearchParams(), // phone 없음
}));

describe("인증코드 페이지 - phone 없을 때", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("전화번호 없이 접근 시 안내 메시지가 표시되어야 한다", async () => {
    const VerifyPage = (await import("@/app/(auth)/signup/verify/page")).default;
    render(<VerifyPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/전화번호 정보가 없습니다/)
      ).toBeInTheDocument();
    });
  });

  it("'회원가입으로 돌아가기' 링크가 표시되어야 한다", async () => {
    const VerifyPage = (await import("@/app/(auth)/signup/verify/page")).default;
    render(<VerifyPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("link", { name: "회원가입으로 돌아가기" })
      ).toBeInTheDocument();
    });
  });
});
