import { describe, it, expect, beforeEach } from "vitest";
import { useAuthStore } from "@/stores/auth-store";
import type { User, AuthTokens } from "@/lib/api/types";

// ========================================
// Auth Store 테스트
// ========================================

const mockUser: User = {
  id: "user-test-001",
  email: "test@rental.com",
  name: "테스트유저",
  phone: "010-0000-0000",
  role: "RENTER",
  createdAt: "2026-01-01T00:00:00Z",
};

const mockTokens: AuthTokens = {
  accessToken: "test-access-token",
  refreshToken: "test-refresh-token",
  expiresIn: 3600,
};

describe("Auth Store", () => {
  beforeEach(() => {
    // 매 테스트마다 스토어 초기화
    const store = useAuthStore.getState();
    store.logout();
    useAuthStore.setState({ isLoading: false });
    // localStorage 클리어
    localStorage.clear();
  });

  it("초기 상태가 올바르게 설정되어야 한다", () => {
    const state = useAuthStore.getState();

    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.error).toBeNull();
  });

  it("setUser를 호출하면 유저가 설정되고 인증 상태가 true가 되어야 한다", () => {
    const { setUser } = useAuthStore.getState();

    setUser(mockUser);

    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
    expect(state.error).toBeNull();
  });

  it("login을 호출하면 유저와 토큰이 저장되어야 한다", () => {
    const { login } = useAuthStore.getState();

    login(mockUser, mockTokens);

    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
    expect(state.error).toBeNull();
  });

  it("logout을 호출하면 상태가 초기화되어야 한다", () => {
    const store = useAuthStore.getState();
    store.login(mockUser, mockTokens);

    // 로그인 확인
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    // 로그아웃
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
  });

  it("setLoading을 호출하면 로딩 상태가 변경되어야 한다", () => {
    const { setLoading } = useAuthStore.getState();

    setLoading(true);
    expect(useAuthStore.getState().isLoading).toBe(true);

    setLoading(false);
    expect(useAuthStore.getState().isLoading).toBe(false);
  });

  it("setError를 호출하면 에러 메시지가 설정되어야 한다", () => {
    const { setError } = useAuthStore.getState();

    setError("테스트 에러 메시지");
    expect(useAuthStore.getState().error).toBe("테스트 에러 메시지");

    setError(null);
    expect(useAuthStore.getState().error).toBeNull();
  });

  it("checkAuth에서 토큰이 없으면 인증되지 않은 상태가 되어야 한다", async () => {
    localStorage.clear();

    const { checkAuth } = useAuthStore.getState();
    await checkAuth();

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
  });
});
