import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  Product,
  ProductSummary,
  CreateProductRequest,
  GuidePriceRange,
  ProductCategory,
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
// Product API 호출
// ========================================

/** 상품 목록 조회 (BE: Page<ProductSummaryResponse>) */
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

  return client.get<SpringPage<ProductSummary>>(ENDPOINTS.PRODUCTS.BASE, {
    params: queryParams,
  });
}

/** 상품 상세 조회 (BE: ProductDetailResponse) */
export async function getProductByIdApi(id: string | number) {
  const client = getApiClient();
  return client.get<Product>(ENDPOINTS.PRODUCTS.BY_ID(id));
}

/** 내 상품 목록 조회 (BE: /api/v1/my-products) */
export async function getMyProductsApi() {
  const client = getApiClient();
  return client.get<SpringPage<ProductSummary>>(ENDPOINTS.PRODUCTS.MY_PRODUCTS);
}

/** 상품 등록 */
export async function createProductApi(request: CreateProductRequest) {
  const client = getApiClient();
  return client.post<Product>(ENDPOINTS.PRODUCTS.BASE, request);
}

/** 상품 수정 */
export async function updateProductApi(
  id: string | number,
  request: Partial<CreateProductRequest>
) {
  const client = getApiClient();
  return client.put<Product>(ENDPOINTS.PRODUCTS.BY_ID(id), request);
}

/** 상품 삭제 */
export async function deleteProductApi(id: string | number) {
  const client = getApiClient();
  return client.delete<void>(ENDPOINTS.PRODUCTS.BY_ID(id));
}

/** 가이드 가격 전체 조회 (BE: /api/v1/price-guides) */
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
