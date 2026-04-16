import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  Rental,
  CreateRentalRequest,
  CreateRentalResponse,
  RejectRentalRequest,
  CancelRentalRequest,
  RentalPaymentRequest,
  MyRentalsParams,
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
// Rental API 호출
// ========================================

/** 대여 신청 (POST /api/v1/rentals) */
export async function createRentalApi(request: CreateRentalRequest) {
  const client = getApiClient();
  return client.post<CreateRentalResponse>(ENDPOINTS.RENTALS.BASE, request);
}

/** 내 대여 목록 조회 (GET /api/v1/my-rentals?role=renter|lender) */
export async function getMyRentalsApi(params?: MyRentalsParams) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};

  if (params?.role) queryParams.role = params.role;
  if (params?.status) queryParams.status = params.status;
  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);

  return client.get<SpringPage<Rental>>(ENDPOINTS.RENTALS.MY_RENTALS, {
    params: queryParams,
  });
}

/** 대여 상세 조회 (GET /api/v1/rentals/:id) */
export async function getRentalDetailApi(id: string | number) {
  const client = getApiClient();
  return client.get<Rental>(ENDPOINTS.RENTALS.BY_ID(id));
}

/** 대여 승인 (PATCH /api/v1/rentals/:id/approve) */
export async function approveRentalApi(id: string | number) {
  const client = getApiClient();
  return client.patch<Rental>(ENDPOINTS.RENTALS.APPROVE(id));
}

/** 대여 거절 (PATCH /api/v1/rentals/:id/reject) */
export async function rejectRentalApi(
  id: string | number,
  request: Omit<RejectRentalRequest, "rentalId">
) {
  const client = getApiClient();
  return client.patch<Rental>(ENDPOINTS.RENTALS.REJECT(id), request);
}

/** 대여 취소 (PATCH /api/v1/rentals/:id/cancel) */
export async function cancelRentalApi(
  id: string | number,
  request?: Omit<CancelRentalRequest, "rentalId">
) {
  const client = getApiClient();
  return client.patch<Rental>(ENDPOINTS.RENTALS.CANCEL(id), request);
}

/** 결제 처리 (POST /api/v1/rentals/:id/payment) */
export async function processPaymentApi(
  id: string | number,
  request: RentalPaymentRequest
) {
  const client = getApiClient();
  return client.post<Rental>(ENDPOINTS.RENTALS.PAYMENT(id), request);
}

/** 대여 시작 — 배송 시작 (PATCH /api/v1/rentals/:id/start) */
export async function startRentalApi(id: string | number) {
  const client = getApiClient();
  return client.patch<Rental>(ENDPOINTS.RENTALS.START(id));
}

/** 반납 처리 (PATCH /api/v1/rentals/:id/return) */
export async function returnRentalApi(id: string | number) {
  const client = getApiClient();
  return client.patch<Rental>(ENDPOINTS.RENTALS.RETURN(id));
}
