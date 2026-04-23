import { getApiClient } from "./client";
import type {
  OpenDisputeRequest,
  DisputeResult,
  DisputeListParams,
  AdminResolveDisputeRequest,
} from "./types";

// ========================================
// Dispute API 엔드포인트 상수
// ========================================

const API_VERSION = "/api/v1";

export const DISPUTE_ENDPOINTS = {
  BASE: `${API_VERSION}/disputes`,
  BY_ID: (id: string | number) => `${API_VERSION}/disputes/${id}`,
  CANCEL: (id: string | number) => `${API_VERSION}/disputes/${id}/cancel`,
  ADMIN_REVIEW: (id: string | number) =>
    `${API_VERSION}/admin/disputes/${id}/review`,
  ADMIN_RESOLVE: (id: string | number) =>
    `${API_VERSION}/admin/disputes/${id}/resolve`,
  ADMIN_LIST: `${API_VERSION}/admin/disputes`,
} as const;

// ========================================
// Dispute API 함수
// ========================================

/** 분쟁 오픈 — POST /api/v1/disputes */
export async function openDisputeApi(request: OpenDisputeRequest) {
  const client = getApiClient();
  return client.post<DisputeResult>(DISPUTE_ENDPOINTS.BASE, request);
}

/** 분쟁 상세 조회 — GET /api/v1/disputes/{id} (opener 또는 ADMIN) */
export async function getDisputeApi(id: string | number) {
  const client = getApiClient();
  return client.get<DisputeResult>(DISPUTE_ENDPOINTS.BY_ID(id));
}

/** 내 분쟁 목록 조회 — GET /api/v1/disputes */
export async function getMyDisputesApi(params?: DisputeListParams) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};
  if (params?.status) queryParams.status = params.status;
  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);
  return client.get<{ content: DisputeResult[]; totalElements: number; totalPages: number; last: boolean }>(
    DISPUTE_ENDPOINTS.BASE,
    { params: queryParams }
  );
}

/** 분쟁 취소 — POST /api/v1/disputes/{id}/cancel (opener + OPEN 상태만) */
export async function cancelDisputeApi(id: string | number) {
  const client = getApiClient();
  return client.post<DisputeResult>(DISPUTE_ENDPOINTS.CANCEL(id));
}

/** 관리자: 분쟁 검토 시작 — PATCH /api/v1/admin/disputes/{id}/review (OPEN→UNDER_REVIEW) */
export async function adminReviewDisputeApi(id: string | number) {
  const client = getApiClient();
  return client.patch<DisputeResult>(DISPUTE_ENDPOINTS.ADMIN_REVIEW(id));
}

/** 관리자: 분쟁 해결 — POST /api/v1/admin/disputes/{id}/resolve */
export async function adminResolveDisputeApi(
  id: string | number,
  request: AdminResolveDisputeRequest
) {
  const client = getApiClient();
  return client.post<DisputeResult>(DISPUTE_ENDPOINTS.ADMIN_RESOLVE(id), request);
}

/** 관리자: 분쟁 목록 조회 — GET /api/v1/admin/disputes */
export async function adminGetDisputesApi(params?: DisputeListParams) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};
  if (params?.status) queryParams.status = params.status;
  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);
  if (params?.sort) queryParams.sort = params.sort;
  return client.get<{ content: DisputeResult[]; totalElements: number; totalPages: number; last: boolean }>(
    DISPUTE_ENDPOINTS.ADMIN_LIST,
    { params: queryParams }
  );
}
