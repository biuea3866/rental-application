import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  Product,
  CreateProductRequest,
  PaginatedResponse,
  GuidePriceRange,
  ProductCategory,
} from "./types";

// ========================================
// Product API 호출
// ========================================

/** 상품 목록 조회 */
export async function getProductsApi(params?: {
  page?: number;
  size?: number;
  category?: ProductCategory;
}) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};

  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);
  if (params?.category) queryParams.category = params.category;

  return client.get<PaginatedResponse<Product>>(ENDPOINTS.PRODUCTS.BASE, {
    params: queryParams,
  });
}

/** 상품 상세 조회 */
export async function getProductByIdApi(id: string) {
  const client = getApiClient();
  return client.get<Product>(ENDPOINTS.PRODUCTS.BY_ID(id));
}

/** 내 상품 목록 조회 */
export async function getMyProductsApi() {
  const client = getApiClient();
  return client.get<PaginatedResponse<Product>>(ENDPOINTS.PRODUCTS.MY_PRODUCTS);
}

/** 상품 등록 */
export async function createProductApi(request: CreateProductRequest) {
  const client = getApiClient();
  return client.post<Product>(ENDPOINTS.PRODUCTS.BASE, request);
}

/** 상품 수정 */
export async function updateProductApi(
  id: string,
  request: Partial<CreateProductRequest>
) {
  const client = getApiClient();
  return client.put<Product>(ENDPOINTS.PRODUCTS.BY_ID(id), request);
}

/** 상품 삭제 */
export async function deleteProductApi(id: string) {
  const client = getApiClient();
  return client.delete<void>(ENDPOINTS.PRODUCTS.BY_ID(id));
}

/** 상품 검색 */
export async function searchProductsApi(query: string) {
  const client = getApiClient();
  return client.get<PaginatedResponse<Product>>(ENDPOINTS.PRODUCTS.SEARCH, {
    params: { q: query },
  });
}

/** 가이드 가격 전체 조회 */
export async function getGuidePricesApi() {
  const client = getApiClient();
  return client.get<GuidePriceRange[]>(ENDPOINTS.GUIDE_PRICES.BASE);
}

/** 카테고리별 가이드 가격 조회 */
export async function getGuidePriceByCategoryApi(category: ProductCategory) {
  const client = getApiClient();
  return client.get<GuidePriceRange>(
    ENDPOINTS.GUIDE_PRICES.BY_CATEGORY(category)
  );
}
