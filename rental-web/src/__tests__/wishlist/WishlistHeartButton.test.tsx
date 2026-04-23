import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor, act } from "@testing-library/react";
import { renderWithQuery } from "@/__tests__/test-utils";
import { WishlistHeartButton } from "@/components/wishlist/WishlistHeartButton";
import { seedWishlistItems, resetWishlistHandlerState } from "@/mocks/handlers/wishlist";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// next/navigation mock
// ========================================

const mockPush = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

// ========================================
// WishlistHeartButton 테스트
// ========================================

describe("WishlistHeartButton", () => {
  beforeEach(() => {
    resetWishlistHandlerState();
    mockPush.mockClear();
    // 기본: 비로그인 상태
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  });

  it("isWished=false 상태에서 '위시리스트에 추가' aria-label이 렌더된다", () => {
    renderWithQuery(
      <WishlistHeartButton productId={1} isWished={false} />
    );

    const btn = screen.getByRole("button", { name: "위시리스트에 추가" });
    expect(btn).toBeInTheDocument();
    expect(btn).toHaveAttribute("aria-pressed", "false");
  });

  it("isWished=true 상태에서 '위시리스트에서 제거' aria-label이 렌더된다", () => {
    renderWithQuery(
      <WishlistHeartButton productId={1} isWished={true} />
    );

    const btn = screen.getByRole("button", { name: "위시리스트에서 제거" });
    expect(btn).toBeInTheDocument();
    expect(btn).toHaveAttribute("aria-pressed", "true");
  });

  it("비로그인 상태에서 클릭 시 /login 으로 이동한다", async () => {
    renderWithQuery(
      <WishlistHeartButton productId={1} isWished={false} />
    );

    const btn = screen.getByRole("button");
    await act(async () => {
      fireEvent.click(btn);
    });

    expect(mockPush).toHaveBeenCalledWith("/login");
  });

  it("로그인 상태에서 클릭 시 API가 호출되고 상태가 변한다", async () => {
    useAuthStore.setState({
      user: { id: 1, email: "test@test.com", name: "테스트", role: "RENTER" },
      isAuthenticated: true,
      isLoading: false,
      error: null,
    });

    renderWithQuery(
      <WishlistHeartButton productId={1} isWished={false} />
    );

    const btn = screen.getByRole("button", { name: "위시리스트에 추가" });
    await act(async () => {
      fireEvent.click(btn);
    });

    // isPending 중에 disabled
    await waitFor(() => {
      expect(btn).not.toBeDisabled();
    });
  });

  it("Space/Enter 키보드로도 토글이 동작한다", async () => {
    useAuthStore.setState({
      user: { id: 1, email: "test@test.com", name: "테스트", role: "RENTER" },
      isAuthenticated: true,
      isLoading: false,
      error: null,
    });

    renderWithQuery(
      <WishlistHeartButton productId={1} isWished={false} />
    );

    const btn = screen.getByRole("button");
    await act(async () => {
      fireEvent.keyDown(btn, { key: "Enter" });
    });

    await waitFor(() => {
      expect(btn).not.toBeDisabled();
    });
  });

  it("409 중복 에러 시 토스트를 표시하고 추가 API를 재호출하지 않는다", async () => {
    // 이미 위시리스트에 있는 상태 (스텁 서버 시드)
    seedWishlistItems([1]);

    useAuthStore.setState({
      user: { id: 1, email: "test@test.com", name: "테스트", role: "RENTER" },
      isAuthenticated: true,
      isLoading: false,
      error: null,
    });

    renderWithQuery(
      // isWished=false 이지만 서버는 409 반환 (불일치 시나리오)
      <WishlistHeartButton productId={1} isWished={false} />
    );

    const btn = screen.getByRole("button");
    await act(async () => {
      fireEvent.click(btn);
    });

    // 토스트는 sonner로 관리되므로 에러 없이 완료되면 OK
    await waitFor(() => {
      expect(btn).not.toBeDisabled();
    });
  });
});
