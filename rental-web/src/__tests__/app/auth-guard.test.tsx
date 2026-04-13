import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthGuard } from "@/components/auth/AuthGuard";

// ========================================
// AuthGuard 테스트
// ========================================

// Next.js navigation mock
const mockReplace = vi.fn();
let mockPathname = "/mypage";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: mockReplace,
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => mockPathname,
  useSearchParams: () => new URLSearchParams(),
}));

// useAuthStore mock
const mockUseAuthStore = vi.fn();

vi.mock("@/stores/auth-store", () => ({
  useAuthStore: (selector?: (state: unknown) => unknown) => mockUseAuthStore(selector),
}));

describe("AuthGuard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // 기본: 비로그인 상태
    mockUseAuthStore.mockReturnValue({ user: null, isAuthenticated: false });
  });

  it("비로그인 사용자가 /mypage 접근 시 /login으로 리다이렉트", () => {
    mockPathname = "/mypage";
    mockUseAuthStore.mockReturnValue({ user: null, isAuthenticated: false });

    render(
      <AuthGuard>
        <div>마이페이지 콘텐츠</div>
      </AuthGuard>
    );

    expect(mockReplace).toHaveBeenCalledWith("/login");
  });

  it("비로그인 사용자가 /lender 접근 시 /login으로 리다이렉트", () => {
    mockPathname = "/lender";
    mockUseAuthStore.mockReturnValue({ user: null, isAuthenticated: false });

    render(
      <AuthGuard>
        <div>등록자 대시보드</div>
      </AuthGuard>
    );

    expect(mockReplace).toHaveBeenCalledWith("/login");
  });

  it("RENTER가 /lender 접근 시 /products로 리다이렉트", () => {
    mockPathname = "/lender";
    mockUseAuthStore.mockReturnValue({
      user: {
        id: "user-renter-001",
        email: "renter@rental.com",
        name: "이빌림",
        phone: "010-3456-7890",
        role: "RENTER",
        createdAt: "2026-01-20T11:00:00Z",
      },
      isAuthenticated: true,
    });

    render(
      <AuthGuard>
        <div>등록자 대시보드</div>
      </AuthGuard>
    );

    expect(mockReplace).toHaveBeenCalledWith("/products");
  });

  it("LENDER가 /products 접근 시 /lender로 리다이렉트", () => {
    mockPathname = "/products";
    mockUseAuthStore.mockReturnValue({
      user: {
        id: "user-lender-001",
        email: "lender@rental.com",
        name: "김대여",
        phone: "010-1234-5678",
        role: "LENDER",
        createdAt: "2026-01-15T09:00:00Z",
      },
      isAuthenticated: true,
    });

    render(
      <AuthGuard>
        <div>상품 목록</div>
      </AuthGuard>
    );

    expect(mockReplace).toHaveBeenCalledWith("/lender");
  });

  it("LENDER가 /lender 접근 시 리다이렉트 없이 콘텐츠 렌더링", () => {
    mockPathname = "/lender";
    mockUseAuthStore.mockReturnValue({
      user: {
        id: "user-lender-001",
        email: "lender@rental.com",
        name: "김대여",
        phone: "010-1234-5678",
        role: "LENDER",
        createdAt: "2026-01-15T09:00:00Z",
      },
      isAuthenticated: true,
    });

    render(
      <AuthGuard>
        <div>등록자 대시보드</div>
      </AuthGuard>
    );

    expect(mockReplace).not.toHaveBeenCalled();
    expect(screen.getByText("등록자 대시보드")).toBeInTheDocument();
  });

  it("RENTER가 /mypage 접근 시 리다이렉트 없이 콘텐츠 렌더링", () => {
    mockPathname = "/mypage";
    mockUseAuthStore.mockReturnValue({
      user: {
        id: "user-renter-001",
        email: "renter@rental.com",
        name: "이빌림",
        phone: "010-3456-7890",
        role: "RENTER",
        createdAt: "2026-01-20T11:00:00Z",
      },
      isAuthenticated: true,
    });

    render(
      <AuthGuard>
        <div>마이페이지 콘텐츠</div>
      </AuthGuard>
    );

    expect(mockReplace).not.toHaveBeenCalled();
    expect(screen.getByText("마이페이지 콘텐츠")).toBeInTheDocument();
  });

  it("비로그인 사용자가 / 접근 시 리다이렉트 없이 콘텐츠 렌더링", () => {
    mockPathname = "/";
    mockUseAuthStore.mockReturnValue({ user: null, isAuthenticated: false });

    render(
      <AuthGuard>
        <div>랜딩 페이지</div>
      </AuthGuard>
    );

    expect(mockReplace).not.toHaveBeenCalled();
    expect(screen.getByText("랜딩 페이지")).toBeInTheDocument();
  });
});
