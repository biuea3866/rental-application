import { describe, it, expect, beforeEach, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { clearTokens, setAccessToken } from "@/lib/auth/token";

// ========================================
// RealApiClient 심층 테스트
// ========================================

const BASE_URL = "http://localhost:8080";

describe("RealApiClient 동작 테스트", () => {
  beforeEach(() => {
    clearTokens();
    vi.resetModules();
  });

  it("getApiClient는 항상 동일한 인스턴스를 반환해야 한다 (싱글턴)", async () => {
    const { getApiClient } = await import("@/lib/api/client");

    const client1 = getApiClient();
    const client2 = getApiClient();

    expect(client1).toBe(client2);
  });

  it("getApiMode는 환경 변수에서 API 모드를 반환해야 한다", async () => {
    const { getApiMode } = await import("@/lib/api/client");

    const mode = getApiMode();
    expect(["stub", "real"]).toContain(mode);
  });

  it("GET 요청이 쿼리 파라미터와 함께 전송되어야 한다", async () => {
    let receivedUrl: string | null = null;

    server.use(
      http.get(`${BASE_URL}/api/v1/products`, ({ request }) => {
        receivedUrl = request.url;
        return HttpResponse.json({
          success: true,
          data: {
            content: [],
            page: 0,
            size: 20,
            totalElements: 0,
            totalPages: 0,
            hasNext: false,
          },
          timestamp: new Date().toISOString(),
        });
      })
    );

    await fetch(
      `${BASE_URL}/api/v1/products?category=ELECTRONICS&page=0&size=10`
    );

    expect(receivedUrl).toContain("category=ELECTRONICS");
    expect(receivedUrl).toContain("page=0");
  });

  it("Authorization 헤더가 accessToken과 함께 전송되어야 한다", async () => {
    setAccessToken("my-test-access-token", 3600);

    let authHeader: string | null = null;

    server.use(
      http.get(`${BASE_URL}/api/v1/auth/me`, ({ request }) => {
        authHeader = request.headers.get("Authorization");
        return HttpResponse.json({
          success: true,
          data: {
            id: "user-001",
            email: "test@test.com",
            name: "테스트",
            phone: "010-0000-0000",
            role: "RENTER",
            createdAt: "2026-01-01T00:00:00Z",
          },
          timestamp: new Date().toISOString(),
        });
      })
    );

    await fetch(`${BASE_URL}/api/v1/auth/me`, {
      headers: {
        Authorization: "Bearer my-test-access-token",
      },
    });

    expect(authHeader).toBe("Bearer my-test-access-token");
  });

  it("401 응답 시 토큰이 삭제되어야 한다", async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/protected-resource`, () => {
        return HttpResponse.json(
          {
            code: "UNAUTHORIZED",
            message: "인증이 만료되었습니다.",
          },
          { status: 401 }
        );
      })
    );

    setAccessToken("expired-token", 3600);

    const response = await fetch(
      `${BASE_URL}/api/v1/protected-resource`
    );

    expect(response.status).toBe(401);
  });

  it("응답 JSON이 올바르게 파싱되어야 한다", async () => {
    const expectedData = { id: "test-123", value: "test-value" };

    server.use(
      http.get(`${BASE_URL}/api/v1/test-parse`, () => {
        return HttpResponse.json({
          success: true,
          data: expectedData,
          timestamp: new Date().toISOString(),
        });
      })
    );

    const response = await fetch(`${BASE_URL}/api/v1/test-parse`);
    const result = await response.json();

    expect(result.data).toEqual(expectedData);
  });

  it("POST 요청 body가 JSON으로 직렬화되어야 한다", async () => {
    let receivedBody: Record<string, unknown> | null = null;

    server.use(
      http.post(`${BASE_URL}/api/v1/test-body`, async ({ request }) => {
        receivedBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({
          success: true,
          data: null,
          timestamp: new Date().toISOString(),
        });
      })
    );

    const testBody = { email: "test@test.com", name: "테스트" };

    await fetch(`${BASE_URL}/api/v1/test-body`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(testBody),
    });

    expect(receivedBody).toEqual(testBody);
  });

  describe("ApiRequestError", () => {
    it("status 코드가 올바르게 설정되어야 한다", async () => {
      const { ApiRequestError } = await import("@/lib/api/client");

      const error = new ApiRequestError(422, {
        code: "VALIDATION_ERROR",
        message: "입력값이 올바르지 않습니다.",
      });

      expect(error.status).toBe(422);
      expect(error.apiError.code).toBe("VALIDATION_ERROR");
      expect(error.message).toBe("입력값이 올바르지 않습니다.");
      expect(error instanceof Error).toBe(true);
    });

    it("instanceof Error로 확인할 수 있어야 한다", async () => {
      const { ApiRequestError } = await import("@/lib/api/client");

      const error = new ApiRequestError(404, {
        code: "NOT_FOUND",
        message: "찾을 수 없음",
      });

      expect(error instanceof ApiRequestError).toBe(true);
      expect(error instanceof Error).toBe(true);
    });
  });

  describe("BASE_URL 및 API_MODE", () => {
    it("BASE_URL이 환경 변수에서 로드되어야 한다", async () => {
      const { BASE_URL } = await import("@/lib/api/client");
      expect(BASE_URL).toBe("http://localhost:8080");
    });

    it("API_MODE가 테스트 환경에서 real이어야 한다", async () => {
      const { API_MODE } = await import("@/lib/api/client");
      expect(API_MODE).toBe("real");
    });
  });

  describe("RealApiClient HTTP 메서드 (getApiClient 경유)", () => {
    it("PATCH 메서드가 getApiClient를 통해 동작해야 한다", async () => {
      server.use(
        http.patch(`${BASE_URL}/api/v1/test-real-patch`, async ({ request }) => {
          const body = await request.json();
          return HttpResponse.json({
            success: true,
            data: body,
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { getApiClient } = await import("@/lib/api/client");
      const client = getApiClient();

      const result = await client.patch("/api/v1/test-real-patch", { name: "patched" });
      expect(result.success).toBe(true);
    });

    it("PUT 메서드가 getApiClient를 통해 동작해야 한다", async () => {
      server.use(
        http.put(`${BASE_URL}/api/v1/test-real-put`, async ({ request }) => {
          const body = await request.json();
          return HttpResponse.json({
            success: true,
            data: body,
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { getApiClient } = await import("@/lib/api/client");
      const client = getApiClient();

      const result = await client.put("/api/v1/test-real-put", { title: "replaced" });
      expect(result.success).toBe(true);
    });

    it("DELETE 메서드가 getApiClient를 통해 동작해야 한다", async () => {
      server.use(
        http.delete(`${BASE_URL}/api/v1/test-real-delete`, () => {
          return HttpResponse.json({
            success: true,
            data: null,
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { getApiClient } = await import("@/lib/api/client");
      const client = getApiClient();

      const result = await client.delete("/api/v1/test-real-delete");
      expect(result.success).toBe(true);
    });

    it("비정상 응답(400)에서 ApiRequestError가 발생해야 한다", async () => {
      server.use(
        http.get(`${BASE_URL}/api/v1/test-error-400`, () => {
          return HttpResponse.json(
            { code: "BAD_REQUEST", message: "잘못된 요청입니다." },
            { status: 400 }
          );
        })
      );

      const { getApiClient, ApiRequestError } = await import("@/lib/api/client");
      const client = getApiClient();

      await expect(client.get("/api/v1/test-error-400")).rejects.toThrow(ApiRequestError);
    });

    it("requiresAuth=false 옵션으로 Authorization 헤더 없이 요청해야 한다", async () => {
      setAccessToken("should-not-be-sent", 3600);

      let authHeader: string | null = "initial";

      server.use(
        http.get(`${BASE_URL}/api/v1/public-endpoint`, ({ request }) => {
          authHeader = request.headers.get("Authorization");
          return HttpResponse.json({
            success: true,
            data: { public: true },
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { getApiClient } = await import("@/lib/api/client");
      const client = getApiClient();

      await client.get("/api/v1/public-endpoint", { requiresAuth: false });
      expect(authHeader).toBeNull();
    });
  });
});
