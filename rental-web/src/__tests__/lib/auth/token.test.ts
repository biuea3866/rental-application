import { describe, it, expect, beforeEach, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";

// ========================================
// Token 관리 테스트
// ========================================

describe("Token 관리", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetModules();
  });

  describe("setAccessToken / getAccessToken", () => {
    it("유효한 accessToken을 저장하고 조회할 수 있어야 한다", async () => {
      const { setAccessToken, getAccessToken } = await import(
        "@/lib/auth/token"
      );

      setAccessToken("test-access-token", 3600);
      expect(getAccessToken()).toBe("test-access-token");
    });

    it("만료된 accessToken은 null을 반환해야 한다", async () => {
      const { setAccessToken, getAccessToken } = await import(
        "@/lib/auth/token"
      );

      // 이미 만료된 토큰 (음수 expiresIn)
      setAccessToken("expired-token", -100);
      expect(getAccessToken()).toBeNull();
    });

    it("토큰이 없으면 null을 반환해야 한다", async () => {
      const { getAccessToken } = await import("@/lib/auth/token");
      expect(getAccessToken()).toBeNull();
    });
  });

  describe("isAccessTokenExpired", () => {
    it("유효한 토큰은 만료되지 않은 상태여야 한다", async () => {
      const { setAccessToken, isAccessTokenExpired } = await import(
        "@/lib/auth/token"
      );

      setAccessToken("valid-token", 3600);
      expect(isAccessTokenExpired()).toBe(false);
    });

    it("만료된 토큰은 만료 상태여야 한다", async () => {
      const { setAccessToken, isAccessTokenExpired } = await import(
        "@/lib/auth/token"
      );

      setAccessToken("expired-token", -100);
      expect(isAccessTokenExpired()).toBe(true);
    });

    it("토큰이 없으면 만료 상태여야 한다", async () => {
      const { isAccessTokenExpired } = await import("@/lib/auth/token");
      expect(isAccessTokenExpired()).toBe(true);
    });
  });

  describe("getRefreshToken / setRefreshToken / removeRefreshToken", () => {
    it("refreshToken을 저장하고 조회할 수 있어야 한다", async () => {
      const { setRefreshToken, getRefreshToken } = await import(
        "@/lib/auth/token"
      );

      setRefreshToken("test-refresh-token");
      expect(getRefreshToken()).toBe("test-refresh-token");
    });

    it("refreshToken을 삭제하면 null이 반환되어야 한다", async () => {
      const { setRefreshToken, getRefreshToken, removeRefreshToken } =
        await import("@/lib/auth/token");

      setRefreshToken("test-refresh-token");
      removeRefreshToken();
      expect(getRefreshToken()).toBeNull();
    });

    it("refreshToken이 없으면 null을 반환해야 한다", async () => {
      const { getRefreshToken } = await import("@/lib/auth/token");
      expect(getRefreshToken()).toBeNull();
    });
  });

  describe("saveTokens / clearTokens", () => {
    it("saveTokens로 accessToken과 refreshToken을 모두 저장해야 한다", async () => {
      const { saveTokens, getAccessToken, getRefreshToken } = await import(
        "@/lib/auth/token"
      );

      saveTokens({
        accessToken: "saved-access",
        refreshToken: "saved-refresh",
        expiresIn: 3600,
      });

      expect(getAccessToken()).toBe("saved-access");
      expect(getRefreshToken()).toBe("saved-refresh");
    });

    it("clearTokens로 모든 토큰을 삭제해야 한다", async () => {
      const { saveTokens, clearTokens, getAccessToken, getRefreshToken } =
        await import("@/lib/auth/token");

      saveTokens({
        accessToken: "access-to-clear",
        refreshToken: "refresh-to-clear",
        expiresIn: 3600,
      });

      clearTokens();

      expect(getAccessToken()).toBeNull();
      expect(getRefreshToken()).toBeNull();
    });
  });

  describe("hasValidTokens", () => {
    it("유효한 accessToken이 있으면 true를 반환해야 한다", async () => {
      const { setAccessToken, hasValidTokens } = await import(
        "@/lib/auth/token"
      );

      setAccessToken("valid-token", 3600);
      expect(hasValidTokens()).toBe(true);
    });

    it("refreshToken만 있어도 true를 반환해야 한다", async () => {
      const { setRefreshToken, hasValidTokens } = await import(
        "@/lib/auth/token"
      );

      setRefreshToken("refresh-only-token");
      expect(hasValidTokens()).toBe(true);
    });

    it("토큰이 없으면 false를 반환해야 한다", async () => {
      const { hasValidTokens } = await import("@/lib/auth/token");
      expect(hasValidTokens()).toBe(false);
    });
  });

  describe("refreshAccessToken", () => {
    it("refreshToken이 없으면 null을 반환해야 한다", async () => {
      const { refreshAccessToken } = await import("@/lib/auth/token");

      const result = await refreshAccessToken();
      expect(result).toBeNull();
    });

    it("유효한 refreshToken으로 토큰 갱신이 성공해야 한다", async () => {
      const { setRefreshToken, refreshAccessToken } = await import(
        "@/lib/auth/token"
      );

      setRefreshToken("valid-refresh-token");

      const result = await refreshAccessToken();

      expect(result).not.toBeNull();
      expect(result?.accessToken).toBeDefined();
      expect(result?.refreshToken).toBeDefined();
    });

    it("갱신 API 실패 시 null을 반환하고 토큰을 삭제해야 한다", async () => {
      server.use(
        http.post("http://localhost:8080/api/v1/auth/refresh", () => {
          return HttpResponse.json(
            { code: "INVALID_TOKEN", message: "유효하지 않은 토큰" },
            { status: 401 }
          );
        })
      );

      const {
        setRefreshToken,
        refreshAccessToken,
        getRefreshToken,
      } = await import("@/lib/auth/token");

      setRefreshToken("invalid-refresh-token");

      const result = await refreshAccessToken();

      expect(result).toBeNull();
      expect(getRefreshToken()).toBeNull();
    });

    it("갱신 요청 중 중복 호출은 동일한 Promise를 반환해야 한다", async () => {
      const { setRefreshToken, refreshAccessToken } = await import(
        "@/lib/auth/token"
      );

      setRefreshToken("valid-refresh-token");

      // 동시에 두 번 호출
      const [result1, result2] = await Promise.all([
        refreshAccessToken(),
        refreshAccessToken(),
      ]);

      // 둘 다 성공해야 한다
      expect(result1).not.toBeNull();
      expect(result2).not.toBeNull();
    });

    it("네트워크 오류 발생 시 null을 반환하고 토큰을 삭제해야 한다", async () => {
      server.use(
        http.post("http://localhost:8080/api/v1/auth/refresh", () => {
          return HttpResponse.error();
        })
      );

      const { setRefreshToken, refreshAccessToken, getRefreshToken } = await import(
        "@/lib/auth/token"
      );

      setRefreshToken("some-refresh-token");

      const result = await refreshAccessToken();

      expect(result).toBeNull();
      expect(getRefreshToken()).toBeNull();
    });
  });
});
