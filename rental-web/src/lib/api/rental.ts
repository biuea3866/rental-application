import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  RentalSummary,
  RentalDetail,
  RequestRentalRequest,
  RequestRentalResponse,
  ApproveRentalResponse,
  RejectOrCancelRentalRequest,
  RejectOrCancelRentalResponse,
  ProcessPaymentRequest,
  ProcessPaymentResponse,
  RentalStatusChangeResponse,
  RentalRole,
  RentalStatus,
} from "./types";

// ========================================
// Spring Page 응답 타입 (대여 목록용)
// ========================================

interface RentalPage {
  content: RentalSummary[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ========================================
// 대여 API 호출
// ========================================

/** 내 대여 목록 조회 — GET /api/v1/rentals?role=&status=&page=&size= */
export async function getMyRentalsApi(params?: {
  role?: RentalRole;
  status?: RentalStatus;
  page?: number;
  size?: number;
}) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};

  if (params?.role) queryParams.role = params.role;
  if (params?.status) queryParams.status = params.status;
  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);

  return client.get<RentalPage>(ENDPOINTS.RENTALS.MY_RENTALS, {
    params: queryParams,
  });
}

/** 대여 상세 조회 — GET /api/v1/rentals/{id} */
export async function getRentalDetailApi(rentalId: string | number) {
  const client = getApiClient();
  return client.get<RentalDetail>(ENDPOINTS.RENTALS.BY_ID(rentalId));
}

/** 대여 신청 — POST /api/v1/rentals */
export async function requestRentalApi(request: RequestRentalRequest) {
  const client = getApiClient();
  return client.post<RequestRentalResponse>(ENDPOINTS.RENTALS.BASE, request);
}

/** 대여 승인 — PATCH /api/v1/rentals/{id}/approve */
export async function approveRentalApi(rentalId: string | number) {
  const client = getApiClient();
  return client.patch<ApproveRentalResponse>(
    ENDPOINTS.RENTALS.APPROVE(rentalId)
  );
}

/** 대여 거절 — PATCH /api/v1/rentals/{id}/reject */
export async function rejectRentalApi(
  rentalId: string | number,
  request: RejectOrCancelRentalRequest
) {
  const client = getApiClient();
  return client.patch<RejectOrCancelRentalResponse>(
    ENDPOINTS.RENTALS.REJECT(rentalId),
    request
  );
}

/** 결제 처리 — POST /api/v1/rentals/{id}/payment */
export async function processPaymentApi(
  rentalId: string | number,
  request: ProcessPaymentRequest
) {
  const client = getApiClient();
  return client.post<ProcessPaymentResponse>(
    ENDPOINTS.RENTALS.PAYMENT(rentalId),
    request
  );
}

/** 배송 시작 — PATCH /api/v1/rentals/{id}/start */
export async function startRentalApi(rentalId: string | number) {
  const client = getApiClient();
  return client.patch<RentalStatusChangeResponse>(
    ENDPOINTS.RENTALS.START(rentalId)
  );
}

/** 반납 처리 — PATCH /api/v1/rentals/{id}/return */
export async function returnRentalApi(rentalId: string | number) {
  const client = getApiClient();
  return client.patch<RentalStatusChangeResponse>(
    ENDPOINTS.RENTALS.RETURN(rentalId)
  );
}

/** 대여 취소 — PATCH /api/v1/rentals/{id}/cancel */
export async function cancelRentalApi(
  rentalId: string | number,
  request: RejectOrCancelRentalRequest
) {
  const client = getApiClient();
  return client.patch<RejectOrCancelRentalResponse>(
    ENDPOINTS.RENTALS.CANCEL(rentalId),
    request
  );
}
