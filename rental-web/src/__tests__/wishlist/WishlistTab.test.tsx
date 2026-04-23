import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor, act } from "@testing-library/react";
import { renderWithQuery } from "@/__tests__/test-utils";
import { WishlistTab } from "@/components/wishlist/WishlistTab";
import {
  seedWishlistItems,
  resetWishlistHandlerState,
} from "@/mocks/handlers/wishlist";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// next/navigation mock
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

// ========================================
// WishlistTab 테스트
// ========================================

describe("WishlistTab", () => {
  beforeEach(() => {
    resetWishlistHandlerState();
    useAuthStore.setState({
      user: { id: 1, email: "test@test.com", name: "테스트", role: "RENTER" },
      isAuthenticated: true,
      isLoading: false,
      error: null,
    });
  });

  // --------------------------------------------------
  // 빈 상태
  // --------------------------------------------------

  it("위시리스트가 비어 있으면 빈 상태 메시지를 렌더한다", async () => {
    renderWithQuery(<WishlistTab />);

    await waitFor(() => {
      expect(
        screen.getByText("위시리스트가 비어 있습니다.")
      ).toBeInTheDocument();
    });
  });

  // --------------------------------------------------
  // 아이템 렌더
  // --------------------------------------------------

  it("위시리스트 아이템이 있으면 그리드에 렌더한다", async () => {
    // productId 1, 2는 stub 상품으로 존재
    seedWishlistItems([1, 2]);

    renderWithQuery(<WishlistTab />);

    await waitFor(() => {
      // 아이템 개수로 확인 (텍스트는 중복 렌더될 수 있으므로 getAllByText 사용)
      const product1Texts = screen.getAllByText("상품 #1");
      expect(product1Texts.length).toBeGreaterThanOrEqual(1);
      const product2Texts = screen.getAllByText("상품 #2");
      expect(product2Texts.length).toBeGreaterThanOrEqual(1);
    });

    // 위시리스트 섹션이 있는지 확인
    expect(screen.getByRole("list", { name: "위시리스트 2개 항목" })).toBeInTheDocument();
  });

  it("각 아이템에 하트 버튼(위시리스트에서 제거)이 렌더된다", async () => {
    seedWishlistItems([1]);

    renderWithQuery(<WishlistTab />);

    await waitFor(() => {
      const heartBtns = screen.getAllByRole("button", {
        name: "위시리스트에서 제거",
      });
      expect(heartBtns.length).toBeGreaterThanOrEqual(1);
    });
  });

  // --------------------------------------------------
  // 페이지네이션
  // --------------------------------------------------

  it("아이템이 1페이지 이내면 페이지네이션이 렌더되지 않는다", async () => {
    // PAGE_SIZE=12 이하
    seedWishlistItems([1, 2, 3]);

    renderWithQuery(<WishlistTab />);

    await waitFor(() => {
      expect(screen.queryByRole("navigation", { name: "위시리스트 페이지네이션" })).not.toBeInTheDocument();
    });
  });

  it("아이템이 PAGE_SIZE 초과면 페이지네이션이 렌더된다", async () => {
    // 13개: 2페이지
    seedWishlistItems(
      Array.from({ length: 13 }, (_, i) => i + 1)
    );

    renderWithQuery(<WishlistTab />);

    await waitFor(() => {
      expect(
        screen.getByRole("navigation", { name: "위시리스트 페이지네이션" })
      ).toBeInTheDocument();
    });
  });

  it("다음 페이지 버튼 클릭 시 페이지가 이동한다", async () => {
    seedWishlistItems(Array.from({ length: 13 }, (_, i) => i + 1));

    renderWithQuery(<WishlistTab />);

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "다음 페이지" })
      ).toBeInTheDocument();
    });

    const nextBtn = screen.getByRole("button", { name: "다음 페이지" });
    await act(async () => {
      fireEvent.click(nextBtn);
    });

    await waitFor(() => {
      // 2페이지 버튼이 aria-current="page"
      const page2Btn = screen.getByRole("button", { name: "2페이지" });
      expect(page2Btn).toHaveAttribute("aria-current", "page");
    });
  });

  it("이전 페이지 버튼은 첫 페이지에서 비활성화된다", async () => {
    seedWishlistItems(Array.from({ length: 13 }, (_, i) => i + 1));

    renderWithQuery(<WishlistTab />);

    await waitFor(() => {
      const prevBtn = screen.getByRole("button", { name: "이전 페이지" });
      expect(prevBtn).toBeDisabled();
    });
  });

  // --------------------------------------------------
  // 낙관적 업데이트 — 삭제 성공 후 빈 상태
  // --------------------------------------------------

  it("삭제 클릭 시 낙관적으로 아이템이 제거되고 서버 확인 후 빈 상태가 된다", async () => {
    seedWishlistItems([1]);

    renderWithQuery(<WishlistTab />);

    // 아이템 로드 대기
    await waitFor(() => {
      expect(screen.getAllByText("상품 #1").length).toBeGreaterThanOrEqual(1);
    });

    const heartBtn = screen.getByRole("button", {
      name: "위시리스트에서 제거",
    });

    // 삭제 클릭
    await act(async () => {
      fireEvent.click(heartBtn);
    });

    // 삭제 후 서버 재조회 → 빈 상태
    await waitFor(() => {
      expect(screen.getByText("위시리스트가 비어 있습니다.")).toBeInTheDocument();
    });
  });
});
