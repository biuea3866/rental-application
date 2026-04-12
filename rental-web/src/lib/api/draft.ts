import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  ProductDraft,
  CreateDraftRequest,
  UpdateDraftRequest,
  PresignedUrlRequest,
  PresignedUrlResponse,
  MyProduct,
  PaginatedResponse,
} from "./types";

// ========================================
// 상품 드래프트 API
// ========================================

/** DRAFT 생성 */
export async function createDraftApi(request: CreateDraftRequest) {
  const client = getApiClient();
  return client.post<ProductDraft>(ENDPOINTS.DRAFTS.BASE, request);
}

/** DRAFT 수정 */
export async function updateDraftApi(id: string, request: UpdateDraftRequest) {
  const client = getApiClient();
  return client.patch<ProductDraft>(ENDPOINTS.DRAFTS.BY_ID(id), request);
}

/** DRAFT 조회 */
export async function getDraftApi(id: string) {
  const client = getApiClient();
  return client.get<ProductDraft>(ENDPOINTS.DRAFTS.BY_ID(id));
}

/** 검수 제출 */
export async function submitDraftApi(id: string) {
  const client = getApiClient();
  return client.post<ProductDraft>(ENDPOINTS.DRAFTS.SUBMIT(id));
}

// ========================================
// 이미지 업로드 API
// ========================================

/** Presigned URL 획득 */
export async function getPresignedUrlApi(request: PresignedUrlRequest) {
  const client = getApiClient();
  return client.post<PresignedUrlResponse>(
    ENDPOINTS.IMAGES.PRESIGNED_URL,
    request
  );
}

/** Presigned URL을 통해 S3에 직접 업로드 */
export async function uploadToS3(
  presignedUrl: string,
  file: File
): Promise<void> {
  const response = await fetch(presignedUrl, {
    method: "PUT",
    body: file,
    headers: {
      "Content-Type": file.type,
    },
  });

  if (!response.ok) {
    throw new Error("이미지 업로드에 실패했습니다.");
  }
}

// ========================================
// 내 상품 목록 API
// ========================================

/** 내 상품 목록 조회 */
export async function getMyProductsApi(params?: {
  page?: number;
  size?: number;
  status?: string;
}) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};

  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);
  if (params?.status) queryParams.status = params.status;

  return client.get<PaginatedResponse<MyProduct>>(ENDPOINTS.MY_PRODUCTS.BASE, {
    params: queryParams,
  });
}
