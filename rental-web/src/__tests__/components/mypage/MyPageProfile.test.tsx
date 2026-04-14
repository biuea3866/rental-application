import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { MyPageProfile } from "@/components/mypage/MyPageProfile";
import type { MyPageInfo } from "@/lib/api/types";

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
  usePathname: () => "/mypage",
}));

// ========================================
// 테스트 픽스처
// ========================================

const LENDER_USER: MyPageInfo = {
  id: "user-lender-001",
  email: "lender@rental.com",
  name: "김대여",
  phone: "010-1234-5678",
  role: "LENDER",
  profileImageUrl: undefined,
  createdAt: "2026-01-15T09:00:00Z",
  lenderProfile: {
    businessName: "김대여 렌탈샵",
    description: "좋은 물건을 합리적인 가격에 빌려드립니다.",
  },
};

const RENTER_USER: MyPageInfo = {
  id: "user-renter-001",
  email: "renter@rental.com",
  name: "이빌림",
  phone: "010-3456-7890",
  role: "RENTER",
  profileImageUrl: undefined,
  createdAt: "2026-01-20T11:00:00Z",
};

// ========================================
// MyPageProfile 테스트
// ========================================

describe("MyPageProfile", () => {
  it("이름이 렌더링되어야 한다", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    expect(screen.getByText("김대여")).toBeInTheDocument();
  });

  it("이메일이 렌더링되어야 한다", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    expect(screen.getByText("lender@rental.com")).toBeInTheDocument();
  });

  it("전화번호가 렌더링되어야 한다", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    expect(screen.getByText("010-1234-5678")).toBeInTheDocument();
  });

  it("등록자 역할이 표시되어야 한다", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    expect(screen.getByText("등록자")).toBeInTheDocument();
  });

  it("대여자 역할이 표시되어야 한다", () => {
    render(<MyPageProfile myPageInfo={RENTER_USER} />);

    expect(screen.getByText("대여자")).toBeInTheDocument();
  });

  it("수정 버튼이 렌더링되어야 한다", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    expect(screen.getByRole("link", { name: "수정" })).toBeInTheDocument();
  });

  it("수정 링크가 /mypage/edit으로 연결되어야 한다", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    const editLink = screen.getByRole("link", { name: "수정" });
    expect(editLink).toHaveAttribute("href", "/mypage/edit");
  });

  it("이름 첫 글자가 아바타에 표시되어야 한다 (이미지 없는 경우)", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    // 이름 "김대여"의 첫 글자 "김"
    expect(screen.getByText("김")).toBeInTheDocument();
  });

  it("가입일이 렌더링되어야 한다", () => {
    render(<MyPageProfile myPageInfo={LENDER_USER} />);

    // 2026-01-15를 한국어 날짜로 표시
    expect(screen.getByText(/2026/)).toBeInTheDocument();
  });
});
