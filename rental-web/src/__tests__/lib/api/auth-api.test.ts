import { describe, it, expect, beforeEach } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { clearTokens, setAccessToken, setRefreshToken } from "@/lib/auth/token";

// ========================================
// Auth API 모듈 테스트
// ========================================

const BASE_URL = "http://localhost:8080";

describe("Auth API 모듈", () => {
  beforeEach(() => {
    clearTokens();
  });

  describe("loginApi", () => {
    it("올바른 자격증명으로 로그인하면 토큰을 반환해야 한다", async () => {
      const { loginApi } = await import("@/lib/api/auth");

      const result = await loginApi({
        email: "lender@rental.com",
        password: "password123",
      });

      expect(result.success).toBe(true);
      expect(result.data.accessToken).toBeDefined();
      expect(result.data.refreshToken).toBeDefined();
      expect(result.data.expiresIn).toBe(3600);
    });

    it("잘못된 비밀번호로 로그인 시 에러가 발생해야 한다", async () => {
      const { loginApi } = await import("@/lib/api/auth");
      const { ApiRequestError } = await import("@/lib/api/client");

      await expect(
        loginApi({ email: "lender@rental.com", password: "wrong_password" })
      ).rejects.toThrow(ApiRequestError);
    });

    it("존재하지 않는 이메일로 로그인 시 에러가 발생해야 한다", async () => {
      const { loginApi } = await import("@/lib/api/auth");
      const { ApiRequestError } = await import("@/lib/api/client");

      await expect(
        loginApi({ email: "notexist@rental.com", password: "password123" })
      ).rejects.toThrow(ApiRequestError);
    });
  });

  describe("signupApi", () => {
    it("새 이메일로 회원가입하면 성공 메시지를 반환해야 한다", async () => {
      const { signupApi } = await import("@/lib/api/auth");

      const result = await signupApi({
        email: "newuser@rental.com",
        password: "password123",
        name: "새사용자",
        phone: "010-9999-0000",
        role: "RENTER",
      });

      expect(result.success).toBe(true);
    });

    it("중복 이메일로 회원가입 시 에러가 발생해야 한다", async () => {
      const { signupApi } = await import("@/lib/api/auth");
      const { ApiRequestError } = await import("@/lib/api/client");

      await expect(
        signupApi({
          email: "lender@rental.com",
          password: "password123",
          name: "중복사용자",
          phone: "010-0000-0000",
          role: "RENTER",
        })
      ).rejects.toThrow(ApiRequestError);
    });
  });

  describe("refreshTokenApi", () => {
    it("유효한 refreshToken으로 토큰 갱신이 성공해야 한다", async () => {
      const { refreshTokenApi } = await import("@/lib/api/auth");

      const result = await refreshTokenApi({
        refreshToken: "stub-refresh-token",
      });

      expect(result.success).toBe(true);
      expect(result.data.accessToken).toBeDefined();
    });
  });

  describe("logoutApi", () => {
    it("로그아웃이 성공해야 한다", async () => {
      // 먼저 토큰 설정
      setAccessToken("test-access-token", 3600);
      setRefreshToken("test-refresh-token");

      const { logoutApi } = await import("@/lib/api/auth");

      const result = await logoutApi();
      expect(result.success).toBe(true);
    });
  });

  describe("getMeApi", () => {
    it("인증 토큰이 있으면 현재 유저 정보를 반환해야 한다", async () => {
      setAccessToken("test-access-token", 3600);

      const { getMeApi } = await import("@/lib/api/auth");

      const result = await getMeApi();

      expect(result.success).toBe(true);
      expect(result.data.email).toBeDefined();
      expect(result.data.id).toBeDefined();
    });
  });

  describe("에러 응답 처리", () => {
    it("500 에러 시 ApiRequestError가 발생해야 한다", async () => {
      server.use(
        http.post(`${BASE_URL}/api/v1/auth/login`, () => {
          return HttpResponse.json(
            { code: "INTERNAL_ERROR", message: "서버 오류" },
            { status: 500 }
          );
        })
      );

      const { loginApi } = await import("@/lib/api/auth");
      const { ApiRequestError } = await import("@/lib/api/client");

      await expect(
        loginApi({ email: "lender@rental.com", password: "password123" })
      ).rejects.toThrow(ApiRequestError);
    });
  });
});
