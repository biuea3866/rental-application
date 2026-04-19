import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  AdminDashboardResponse,
  AdminRentalListResponse,
} from "./types";

// ========================================
// 관리자 API 호출
// ========================================

/** 1. 관리자 대시보드 조회 — GET /api/v1/admin/dashboard */
export async function getAdminDashboardApi() {
  const client = getApiClient();
  return client.get<AdminDashboardResponse>(ENDPOINTS.ADMIN.DASHBOARD);
}

/** 2. 관리자 대여 목록 조회 — GET /api/v1/admin/rentals?page=&size= */
export async function getAdminRentalsApi(params?: {
  page?: number;
  size?: number;
  status?: string;
}) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};
  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);
  if (params?.status) queryParams.status = params.status;
  return client.get<AdminRentalListResponse>(ENDPOINTS.ADMIN.RENTALS, {
    params: queryParams,
  });
}

/** 3. 유저 정지 — POST /api/v1/admin/users/{userId}/suspend */
export async function suspendUserApi(userId: string | number) {
  const client = getApiClient();
  return client.post<void>(ENDPOINTS.ADMIN.SUSPEND_USER(userId));
}

/** 4. 유저 활성화 — POST /api/v1/admin/users/{userId}/activate */
export async function activateUserApi(userId: string | number) {
  const client = getApiClient();
  return client.post<void>(ENDPOINTS.ADMIN.ACTIVATE_USER(userId));
}
