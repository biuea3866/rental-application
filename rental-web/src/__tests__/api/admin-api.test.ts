import { describe, it, expect, vi, beforeEach } from "vitest";

// ========================================
// Admin API 함수 단위 테스트 (TDD)
// RC-FE-328
// ========================================

const sharedMockClient = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
};

vi.mock("@/lib/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/client")>();
  return {
    ...actual,
    getApiClient: () => sharedMockClient,
  };
});

describe("Admin API 함수", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // TC-ADMIN-01: getAdminDashboardApi — 대시보드 조회
  it("TC-ADMIN-01: getAdminDashboardApi — GET /api/v1/admin/dashboard 호출", async () => {
    const { getAdminDashboardApi } = await import("@/lib/api/admin");
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: {
        rentalCounts: { REQUESTED: 5 },
        dailyRevenue: [],
        weeklyRevenue: [],
      },
      timestamp: new Date().toISOString(),
    });

    const result = await getAdminDashboardApi();

    expect(sharedMockClient.get).toHaveBeenCalledWith("/api/v1/admin/dashboard");
    expect(result.data.rentalCounts.REQUESTED).toBe(5);
  });

  // TC-ADMIN-02: getAdminRentalsApi — 대여 목록 조회
  it("TC-ADMIN-02: getAdminRentalsApi — GET /api/v1/admin/rentals 호출", async () => {
    const { getAdminRentalsApi } = await import("@/lib/api/admin");
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: { content: [], totalElements: 0, totalPages: 0 },
      timestamp: new Date().toISOString(),
    });

    await getAdminRentalsApi({ page: 0, size: 20, status: "IN_USE" });

    expect(sharedMockClient.get).toHaveBeenCalledWith(
      "/api/v1/admin/rentals",
      { params: { page: "0", size: "20", status: "IN_USE" } }
    );
  });

  // TC-ADMIN-03: suspendUserApi — 유저 정지
  it("TC-ADMIN-03: suspendUserApi — POST /api/v1/admin/users/{userId}/suspend 호출", async () => {
    const { suspendUserApi } = await import("@/lib/api/admin");
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: undefined,
      timestamp: new Date().toISOString(),
    });

    await suspendUserApi(42);

    expect(sharedMockClient.post).toHaveBeenCalledWith(
      "/api/v1/admin/users/42/suspend"
    );
  });

  // TC-ADMIN-04: activateUserApi — 유저 활성화
  it("TC-ADMIN-04: activateUserApi — POST /api/v1/admin/users/{userId}/activate 호출", async () => {
    const { activateUserApi } = await import("@/lib/api/admin");
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: undefined,
      timestamp: new Date().toISOString(),
    });

    await activateUserApi(42);

    expect(sharedMockClient.post).toHaveBeenCalledWith(
      "/api/v1/admin/users/42/activate"
    );
  });
});
