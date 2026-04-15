import { describe, it, expect, beforeEach } from "vitest";
import { useAuthStore } from "@/stores/auth-store";
import {
  setAccessToken,
  setRefreshToken,
  setTokenFamily,
  clearTokens,
} from "@/lib/auth/token";
import type { User, AuthTokens } from "@/lib/api/types";

// ========================================
// Auth Store 확장 테스트 (미커버 브랜치)
// ========================================

const mockUser: User = {
  id: 1,
  email: "test@rental.com",
  name: "테스트유저",
  role: "RENTER",
};

const mockTokens: AuthTokens = {
  accessToken: "test-access-token",
  refreshToken: "test-refresh-token",
  tokenFamily: "family-001",
  userId: 1,
};

describe("Auth Store 확장 테스트", () => {
  beforeEach(() => {
    const store = useAuthStore.getState();
    store.logout();
    useAuthStore.setState({ isLoading: false });
    clearTokens();
  });

  describe("setTokens", () => {
    it("setTokens를 호출하면 토큰이 저장되어야 한다", () => {
      const { setTokens } = useAuthStore.getState();

      setTokens(mockTokens);

      // localStorage에 refreshToken이 저장되어야 한다
      expect(localStorage.getItem("rental_refresh_token")).toBe(
        "test-refresh-token"
      );
    });
  });

  describe("checkAuth - 유효한 accessToken", () => {
    it("유효한 accessToken이 있으면 인증 상태가 true여야 한다", async () => {
      // 유효한 토큰 설정
      setAccessToken("valid-access-token", 3_600_000);
      setRefreshToken("valid-refresh-token");

      const { checkAuth } = useAuthStore.getState();
      await checkAuth();

      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(true);
      expect(state.isLoading).toBe(false);
    });
  });

  describe("checkAuth - accessToken 만료, refreshToken 있음", () => {
    it("accessToken이 만료되고 refreshToken으로 갱신 성공 시 인증 상태가 true여야 한다", async () => {
      // 만료된 accessToken, 유효한 refreshToken + tokenFamily
      setRefreshToken("valid-refresh-token");
      setTokenFamily("valid-family-001");
      // accessToken은 저장하지 않아서 만료 상태

      const { checkAuth } = useAuthStore.getState();
      await checkAuth();

      const state = useAuthStore.getState();
      // refreshToken으로 갱신 성공 → 인증 상태 true
      expect(state.isAuthenticated).toBe(true);
      expect(state.isLoading).toBe(false);
    });

    it("accessToken이 만료되고 refreshToken 갱신 실패 시 인증 상태가 false여야 한다", async () => {
      // refreshToken은 있지만 갱신 실패 시뮬레이션
      // refreshAccessToken을 모킹
      const { http, HttpResponse } = await import("msw");
      const { server } = await import("@/mocks/server");

      server.use(
        http.post("http://localhost:8080/api/v1/auth/refresh", () => {
          return HttpResponse.json(
            { code: "INVALID_TOKEN", message: "유효하지 않은 토큰" },
            { status: 401 }
          );
        })
      );

      setRefreshToken("invalid-refresh-token");
      setTokenFamily("invalid-family-001");

      const { checkAuth } = useAuthStore.getState();
      await checkAuth();

      const state = useAuthStore.getState();
      // 갱신 실패 → 인증 상태 false
      expect(state.isAuthenticated).toBe(false);
      expect(state.isLoading).toBe(false);
    });
  });

  describe("checkAuth - 모든 토큰 없음", () => {
    it("토큰이 전혀 없으면 인증 상태가 false여야 한다", async () => {
      clearTokens();

      const { checkAuth } = useAuthStore.getState();
      await checkAuth();

      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(false);
      expect(state.isLoading).toBe(false);
    });
  });

  describe("login", () => {
    it("login을 호출하면 user, isAuthenticated가 설정되고 isLoading이 false여야 한다", () => {
      const { login } = useAuthStore.getState();
      login(mockUser, mockTokens);

      const state = useAuthStore.getState();
      expect(state.user).toEqual(mockUser);
      expect(state.isAuthenticated).toBe(true);
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
    });
  });

  describe("logout", () => {
    it("logout 시 user가 null이 되고 isAuthenticated가 false여야 한다", () => {
      const store = useAuthStore.getState();
      store.login(mockUser, mockTokens);

      store.logout();

      const state = useAuthStore.getState();
      expect(state.user).toBeNull();
      expect(state.isAuthenticated).toBe(false);
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
    });

    it("logout 시 localStorage의 refreshToken이 삭제되어야 한다", () => {
      setRefreshToken("refresh-to-remove");
      expect(localStorage.getItem("rental_refresh_token")).toBe(
        "refresh-to-remove"
      );

      const { logout } = useAuthStore.getState();
      logout();

      expect(localStorage.getItem("rental_refresh_token")).toBeNull();
    });
  });
});
