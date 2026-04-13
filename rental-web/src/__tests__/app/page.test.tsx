import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import HomePage from "@/app/page";

// ========================================
// 홈 페이지 테스트
// ========================================

// Next.js navigation mock
const mockReplace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: mockReplace,
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/",
  useSearchParams: () => new URLSearchParams(),
}));

// useAuthStore mock
const mockUseAuthStore = vi.fn();

vi.mock("@/stores/auth-store", () => ({
  useAuthStore: (selector?: (state: unknown) => unknown) => mockUseAuthStore(selector),
}));

describe("HomePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // 기본값: 비로그인 상태
    mockUseAuthStore.mockReturnValue({ user: null, isAuthenticated: false });
  });

  // ----------------------------------------
  // 렌더링 테스트
  // ----------------------------------------

  it("메인 타이틀이 렌더링되어야 한다", () => {
    render(<HomePage />);
    expect(screen.getByText("Rental Commerce")).toBeInTheDocument();
  });

  it("플랫폼 설명이 표시되어야 한다", () => {
    render(<HomePage />);
    expect(
      screen.getByText("물건을 빌려주고 빌리는 대여 커머스 플랫폼")
    ).toBeInTheDocument();
  });

  it("등록자 카드가 렌더링되어야 한다", () => {
    render(<HomePage />);
    expect(screen.getByText("등록자 (Lender)")).toBeInTheDocument();
    expect(screen.getByText("등록자로 시작하기")).toBeInTheDocument();
  });

  it("대여자 카드가 렌더링되어야 한다", () => {
    render(<HomePage />);
    expect(screen.getByText("대여자 (Renter)")).toBeInTheDocument();
    expect(screen.getByText("대여자로 시작하기")).toBeInTheDocument();
  });

  it("로그인 페이지로 향하는 링크가 있어야 한다", () => {
    render(<HomePage />);
    const links = screen.getAllByRole("link");
    const loginLinks = links.filter(
      (link) => link.getAttribute("href") === "/login"
    );
    expect(loginLinks).toHaveLength(2);
  });

  // ----------------------------------------
  // 역할 기반 리다이렉트 테스트
  // ----------------------------------------

  it("비로그인 상태면 랜딩 페이지 표시", () => {
    mockUseAuthStore.mockReturnValue({ user: null, isAuthenticated: false });
    render(<HomePage />);

    expect(mockReplace).not.toHaveBeenCalled();
    expect(screen.getByText("Rental Commerce")).toBeInTheDocument();
  });

  it("LENDER로 로그인한 상태면 /lender로 리다이렉트", () => {
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

    render(<HomePage />);

    expect(mockReplace).toHaveBeenCalledWith("/lender");
  });

  it("RENTER로 로그인한 상태면 /products로 리다이렉트", () => {
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

    render(<HomePage />);

    expect(mockReplace).toHaveBeenCalledWith("/products");
  });
});
