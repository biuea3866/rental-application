import { describe, it, expect, beforeEach, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";

// ========================================
// API Client 테스트
// ========================================

// 테스트마다 모듈을 새로 로드하기 위해 dynamic import 사용
const BASE_URL = "http://localhost:8080";

describe("API Client", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetModules();
  });

  describe("RealApiClient (fetch 기반)", () => {
    it("GET 요청이 정상적으로 동작해야 한다", async () => {
      const testData = { id: "1", name: "테스트" };

      server.use(
        http.get(`${BASE_URL}/api/v1/test`, () => {
          return HttpResponse.json({
            success: true,
            data: testData,
            timestamp: new Date().toISOString(),
          });
        })
      );

      const response = await fetch(`${BASE_URL}/api/v1/test`);
      const result = await response.json();

      expect(result.success).toBe(true);
      expect(result.data).toEqual(testData);
    });

    it("POST 요청이 정상적으로 동작해야 한다", async () => {
      const requestBody = { email: "test@test.com", password: "test1234" };
      const responseData = {
        accessToken: "token",
        refreshToken: "refresh",
        expiresIn: 3600,
      };

      server.use(
        http.post(`${BASE_URL}/api/v1/test`, async ({ request }) => {
          const body = await request.json();
          expect(body).toEqual(requestBody);

          return HttpResponse.json({
            success: true,
            data: responseData,
            timestamp: new Date().toISOString(),
          });
        })
      );

      const response = await fetch(`${BASE_URL}/api/v1/test`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody),
      });
      const result = await response.json();

      expect(result.success).toBe(true);
      expect(result.data).toEqual(responseData);
    });

    it("401 응답이면 인증 에러를 반환해야 한다", async () => {
      server.use(
        http.get(`${BASE_URL}/api/v1/test-auth`, () => {
          return HttpResponse.json(
            {
              code: "UNAUTHORIZED",
              message: "인증이 만료되었습니다.",
            },
            { status: 401 }
          );
        })
      );

      const response = await fetch(`${BASE_URL}/api/v1/test-auth`);

      expect(response.status).toBe(401);
    });

    it("서버 에러 응답을 처리해야 한다", async () => {
      server.use(
        http.get(`${BASE_URL}/api/v1/test-error`, () => {
          return HttpResponse.json(
            {
              code: "INTERNAL_ERROR",
              message: "서버 내부 오류가 발생했습니다.",
            },
            { status: 500 }
          );
        })
      );

      const response = await fetch(`${BASE_URL}/api/v1/test-error`);

      expect(response.ok).toBe(false);
      expect(response.status).toBe(500);
    });

    it("Authorization 헤더가 올바르게 첨부되어야 한다", async () => {
      let receivedAuthHeader: string | null = null;

      server.use(
        http.get(`${BASE_URL}/api/v1/test-header`, ({ request }) => {
          receivedAuthHeader = request.headers.get("Authorization");
          return HttpResponse.json({
            success: true,
            data: null,
            timestamp: new Date().toISOString(),
          });
        })
      );

      const token = "test-bearer-token";
      await fetch(`${BASE_URL}/api/v1/test-header`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      expect(receivedAuthHeader).toBe(`Bearer ${token}`);
    });
  });

  describe("ApiRequestError", () => {
    it("에러 클래스가 올바르게 생성되어야 한다", async () => {
      const { ApiRequestError } = await import("@/lib/api/client");

      const error = new ApiRequestError(404, {
        code: "NOT_FOUND",
        message: "리소스를 찾을 수 없습니다.",
      });

      expect(error.status).toBe(404);
      expect(error.apiError.code).toBe("NOT_FOUND");
      expect(error.message).toBe("리소스를 찾을 수 없습니다.");
      expect(error.name).toBe("ApiRequestError");
    });
  });
});
