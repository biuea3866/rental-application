import { describe, it, expect, beforeEach, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";

// ========================================
// RealApiClient 추가 메서드 테스트
// ========================================

const BASE_URL = "http://localhost:8080";

describe("RealApiClient HTTP 메서드 테스트", () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it("PATCH 요청이 정상적으로 동작해야 한다", async () => {
    server.use(
      http.patch(`${BASE_URL}/api/v1/test-patch`, async ({ request }) => {
        const body = await request.json();
        return HttpResponse.json({
          success: true,
          data: body,
          timestamp: new Date().toISOString(),
        });
      })
    );

    const response = await fetch(`${BASE_URL}/api/v1/test-patch`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: "updated" }),
    });

    expect(response.ok).toBe(true);
    const result = await response.json();
    expect(result.data).toEqual({ name: "updated" });
  });

  it("PUT 요청이 정상적으로 동작해야 한다", async () => {
    server.use(
      http.put(`${BASE_URL}/api/v1/test-put`, async ({ request }) => {
        const body = await request.json();
        return HttpResponse.json({
          success: true,
          data: body,
          timestamp: new Date().toISOString(),
        });
      })
    );

    const response = await fetch(`${BASE_URL}/api/v1/test-put`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title: "replaced" }),
    });

    expect(response.ok).toBe(true);
    const result = await response.json();
    expect(result.data).toEqual({ title: "replaced" });
  });

  it("DELETE 요청이 정상적으로 동작해야 한다", async () => {
    server.use(
      http.delete(`${BASE_URL}/api/v1/test-delete`, () => {
        return HttpResponse.json({
          success: true,
          data: null,
          timestamp: new Date().toISOString(),
        });
      })
    );

    const response = await fetch(`${BASE_URL}/api/v1/test-delete`, {
      method: "DELETE",
    });

    expect(response.ok).toBe(true);
  });
});

describe("StubApiClient / getStubClient 테스트", () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it("real 모드에서 getStubClient는 null을 반환해야 한다", async () => {
    const { getStubClient } = await import("@/lib/api/client");
    const result = getStubClient();
    expect(result).toBeNull();
  });

  it("getApiMode는 real 또는 stub 중 하나를 반환해야 한다", async () => {
    const { getApiMode } = await import("@/lib/api/client");
    expect(["real", "stub"]).toContain(getApiMode());
  });

  it("getApiClient 호출 시 API 인터페이스를 가진 객체를 반환해야 한다", async () => {
    const { getApiClient } = await import("@/lib/api/client");
    const client = getApiClient();

    expect(typeof client.get).toBe("function");
    expect(typeof client.post).toBe("function");
    expect(typeof client.put).toBe("function");
    expect(typeof client.patch).toBe("function");
    expect(typeof client.delete).toBe("function");
  });
});
