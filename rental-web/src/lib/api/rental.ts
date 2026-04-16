import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  CreateRentalRequest,
  RentalCreatedResponse,
  RentalDetail,
  RentalSummary,
  RentalApproveResponse,
  RentalRejectRequest,
  RentalCancelResponse,
  ProcessPaymentRequest,
  ProcessPaymentResponse,
  RentalStartResponse,
  RentalReturnResponse,
} from "./types";

// ========================================
// Spring Page 응답 타입
// ========================================

interface SpringPage<T> {
  content: T[];
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ========================================
// Rental API 호출 (9개 함수)
// ========================================

/** 1. 대여 신청 — POST /api/v1/rentals */
export async function createRentalApi(request: CreateRentalRequest) {
  const client = getApiClient();
  return client.post<RentalCreatedResponse>(ENDPOINTS.RENTALS.BASE, request);
}

/** 2. 대여 목록 조회 — GET /api/v1/rentals?role=&status=&page=&size= */
export async function getRentalsApi(params?: {
  role?: "RENTER" | "LENDER";
  status?: string;
  page?: number;
  size?: number;
}) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};
  if (params?.role) queryParams.role = params.role;
  if (params?.status) queryParams.status = params.status;
  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);
  return client.get<SpringPage<RentalSummary>>(ENDPOINTS.RENTALS.BASE, {
    params: queryParams,
  });
}

/** 3. 대여 상세 조회 — GET /api/v1/rentals/{rentalId} */
export async function getRentalByIdApi(rentalId: string | number) {
  const client = getApiClient();
  return client.get<RentalDetail>(ENDPOINTS.RENTALS.BY_ID(rentalId));
}

/** 4. 대여 승인 — PATCH /api/v1/rentals/{rentalId}/approve */
export async function approveRentalApi(rentalId: string | number) {
  const client = getApiClient();
  return client.patch<RentalApproveResponse>(
    ENDPOINTS.RENTALS.APPROVE(rentalId)
  );
}

/** 5. 대여 거절 — PATCH /api/v1/rentals/{rentalId}/reject */
export async function rejectRentalApi(
  rentalId: string | number,
  request: RentalRejectRequest
) {
  const client = getApiClient();
  return client.patch<RentalCancelResponse>(
    ENDPOINTS.RENTALS.REJECT(rentalId),
    request
  );
}

/** 6. 결제 처리 — POST /api/v1/rentals/{rentalId}/payment */
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

/** 7. 대여 시작 (배송 시작) — PATCH /api/v1/rentals/{rentalId}/start */
export async function startRentalApi(rentalId: string | number) {
  const client = getApiClient();
  return client.patch<RentalStartResponse>(ENDPOINTS.RENTALS.START(rentalId));
}

/** 8. 반납 처리 — PATCH /api/v1/rentals/{rentalId}/return */
export async function returnRentalApi(rentalId: string | number) {
  const client = getApiClient();
  return client.patch<RentalReturnResponse>(ENDPOINTS.RENTALS.RETURN(rentalId));
}

/** 9. 대여 취소 — PATCH /api/v1/rentals/{rentalId}/cancel */
export async function cancelRentalApi(
  rentalId: string | number,
  request: RentalRejectRequest
) {
  const client = getApiClient();
  return client.patch<RentalCancelResponse>(
    ENDPOINTS.RENTALS.CANCEL(rentalId),
    request
  );
}
