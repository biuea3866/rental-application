import { render, screen, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthProvider } from "@/providers/auth-provider";
import { useAuthStore } from "@/stores/auth-store";
import { clearTokens } from "@/lib/auth/token";

// ========================================
// AuthProvider 테스트
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/",
}));

describe("AuthProvider", () => {
  beforeEach(() => {
    clearTokens();
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
    vi.clearAllMocks();
  });

  it("로딩 중이 아닐 때 children을 렌더링해야 한다", async () => {
    // checkAuth가 즉시 완료되도록 모킹
    const checkAuthMock = vi.fn().mockResolvedValue(undefined);
    useAuthStore.setState({ isLoading: false, checkAuth: checkAuthMock });

    render(
      <AuthProvider>
        <div>테스트 자식 컴포넌트</div>
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("테스트 자식 컴포넌트")).toBeInTheDocument();
    });
  });

  it("isLoading이 true이고 checkAuth가 로딩 상태를 유지하는 동안 로딩 화면을 표시해야 한다", async () => {
    // checkAuth 호출 시 isLoading=true 유지
    let resolveCheckAuth: () => void;
    const checkAuthMock = vi.fn().mockReturnValue(
      new Promise<void>((resolve) => {
        resolveCheckAuth = resolve;
      })
    );

    useAuthStore.setState({ isLoading: false, checkAuth: checkAuthMock });

    render(
      <AuthProvider>
        <div>자식 내용</div>
      </AuthProvider>
    );

    // checkAuth 호출되면 isLoading true로 설정 (실제 구현에서는 내부에서 set)
    await act(async () => {
      useAuthStore.setState({ isLoading: true });
    });

    expect(screen.getByText("로딩 중...")).toBeInTheDocument();

    // cleanup - resolve the promise
    await act(async () => {
      resolveCheckAuth!();
      useAuthStore.setState({ isLoading: false });
    });
  });

  it("로딩 중일 때 children이 렌더링되지 않아야 한다", async () => {
    let resolveCheckAuth: () => void;
    const checkAuthMock = vi.fn().mockReturnValue(
      new Promise<void>((resolve) => {
        resolveCheckAuth = resolve;
      })
    );

    useAuthStore.setState({ isLoading: false, checkAuth: checkAuthMock });

    render(
      <AuthProvider>
        <div>자식 내용</div>
      </AuthProvider>
    );

    await act(async () => {
      useAuthStore.setState({ isLoading: true });
    });

    expect(screen.queryByText("자식 내용")).not.toBeInTheDocument();

    await act(async () => {
      resolveCheckAuth!();
      useAuthStore.setState({ isLoading: false });
    });
  });

  it("checkAuth를 마운트 시 호출해야 한다", async () => {
    const checkAuthMock = vi.fn().mockResolvedValue(undefined);
    useAuthStore.setState({ isLoading: false, checkAuth: checkAuthMock });

    render(
      <AuthProvider>
        <div>내용</div>
      </AuthProvider>
    );

    await waitFor(() => {
      expect(checkAuthMock).toHaveBeenCalled();
    });
  });
});
