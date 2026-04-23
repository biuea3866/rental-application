// ========================================
// FE-452: 검색 필터/정렬 타입 정의
// BE ProductSearchCondition 기준
// ========================================

import type { ProductCategory, RentalUnit } from "./types";

/** BE ProductSearchCondition.sortBy */
export type SortBy =
  | "CREATED_AT"
  | "NAME"
  | "DEPOSIT_AMOUNT"
  | "RATING_AVG"
  | "RENTAL_COUNT";

/** BE ProductSearchCondition.sortDirection */
export type SortDirection = "ASC" | "DESC";

/** 지역 코드 (BE 기준) */
export type RegionCode =
  | "SEOUL_GANGNAM"
  | "SEOUL_GANGBUK"
  | "SEOUL_GANGDONG"
  | "SEOUL_GANGSO"
  | "BUSAN"
  | "DAEGU"
  | "INCHEON"
  | "GWANGJU"
  | "DAEJEON"
  | "ULSAN"
  | "GYEONGGI"
  | "OTHERS";

/** 지역 코드 → 한글 레이블 */
export const REGION_LABELS: Record<RegionCode, string> = {
  SEOUL_GANGNAM: "서울 강남",
  SEOUL_GANGBUK: "서울 강북",
  SEOUL_GANGDONG: "서울 강동",
  SEOUL_GANGSO: "서울 강서",
  BUSAN: "부산",
  DAEGU: "대구",
  INCHEON: "인천",
  GWANGJU: "광주",
  DAEJEON: "대전",
  ULSAN: "울산",
  GYEONGGI: "경기도",
  OTHERS: "기타",
};

/** BE ProductSearchCondition 전체 */
export interface ProductSearchCondition {
  keyword?: string;
  /** 다중 카테고리 선택 */
  categoryCodes?: ProductCategory[];
  regionCode?: RegionCode;
  minPrice?: number;
  maxPrice?: number;
  rentalUnit?: RentalUnit;
  sortBy?: SortBy;
  sortDirection?: SortDirection;
  page?: number;
  size?: number;
}

/** URL 쿼리 파라미터 키-값 (모두 string) */
export type SearchQueryParams = Partial<{
  keyword: string;
  categoryCodes: string; // comma-separated
  regionCode: string;
  minPrice: string;
  maxPrice: string;
  rentalUnit: string;
  sortBy: string;
  sortDirection: string;
  page: string;
  size: string;
}>;

/** URL 쿼리 → ProductSearchCondition 변환 */
export function parseSearchParams(
  params: Record<string, string | string[] | undefined>
): ProductSearchCondition {
  const get = (key: string): string | undefined => {
    const v = params[key];
    return typeof v === "string" ? v : Array.isArray(v) ? v[0] : undefined;
  };

  // categoryCodes: string("A,B"), string[]("A","B"), string[]("A,B,C") 모두 지원
  const rawCategoryCodes = params["categoryCodes"];
  const parsedCategoryCodes: ProductCategory[] = (() => {
    if (!rawCategoryCodes) return [];
    if (typeof rawCategoryCodes === "string") {
      return rawCategoryCodes.split(",").filter(Boolean) as ProductCategory[];
    }
    // string[] — 각 요소가 comma-separated일 수도 있음
    return rawCategoryCodes
      .flatMap((c) => c.split(",").filter(Boolean)) as ProductCategory[];
  })();

  return {
    keyword: get("keyword"),
    categoryCodes: parsedCategoryCodes.length > 0 ? parsedCategoryCodes : undefined,
    regionCode: get("regionCode") as RegionCode | undefined,
    minPrice: get("minPrice") ? Number(get("minPrice")) : undefined,
    maxPrice: get("maxPrice") ? Number(get("maxPrice")) : undefined,
    rentalUnit: get("rentalUnit") as RentalUnit | undefined,
    sortBy: (get("sortBy") as SortBy) ?? "CREATED_AT",
    sortDirection: (get("sortDirection") as SortDirection) ?? "DESC",
    page: get("page") ? Number(get("page")) : 0,
    size: get("size") ? Number(get("size")) : 20,
  };
}

/** ProductSearchCondition → URLSearchParams 변환 */
export function conditionToSearchParams(
  condition: ProductSearchCondition
): URLSearchParams {
  const params = new URLSearchParams();

  if (condition.keyword) params.set("keyword", condition.keyword);
  if (condition.categoryCodes?.length) {
    params.set("categoryCodes", condition.categoryCodes.join(","));
  }
  if (condition.regionCode) params.set("regionCode", condition.regionCode);
  if (condition.minPrice !== undefined) {
    params.set("minPrice", String(condition.minPrice));
  }
  if (condition.maxPrice !== undefined) {
    params.set("maxPrice", String(condition.maxPrice));
  }
  if (condition.rentalUnit) params.set("rentalUnit", condition.rentalUnit);
  if (condition.sortBy && condition.sortBy !== "CREATED_AT") {
    params.set("sortBy", condition.sortBy);
  }
  if (condition.sortDirection && condition.sortDirection !== "DESC") {
    params.set("sortDirection", condition.sortDirection);
  }
  if (condition.page) params.set("page", String(condition.page));

  return params;
}

/** ProductSearchCondition → BE API 쿼리 파라미터 변환 */
export function conditionToApiParams(
  condition: ProductSearchCondition
): Record<string, string> {
  const params: Record<string, string> = {};

  if (condition.keyword) params.keyword = condition.keyword;
  if (condition.categoryCodes?.length) {
    // BE는 categoryCodes를 반복 파라미터로 받을 수도 있으나,
    // 여기서는 comma-separated string으로 전송
    params.categoryCodes = condition.categoryCodes.join(",");
  }
  if (condition.regionCode) params.regionCode = condition.regionCode;
  if (condition.minPrice !== undefined) {
    params.minPrice = String(condition.minPrice);
  }
  if (condition.maxPrice !== undefined) {
    params.maxPrice = String(condition.maxPrice);
  }
  if (condition.rentalUnit) params.rentalUnit = condition.rentalUnit;
  if (condition.sortBy) params.sortBy = condition.sortBy;
  if (condition.sortDirection) params.sortDirection = condition.sortDirection;
  params.page = String(condition.page ?? 0);
  params.size = String(condition.size ?? 20);

  return params;
}

// ========================================
// 정렬 옵션 상수
// ========================================

export interface SortOption {
  sortBy: SortBy;
  label: string;
}

export const SORT_OPTIONS: SortOption[] = [
  { sortBy: "CREATED_AT", label: "최신" },
  { sortBy: "NAME", label: "이름" },
  { sortBy: "DEPOSIT_AMOUNT", label: "보증금" },
  { sortBy: "RATING_AVG", label: "평점" },
  { sortBy: "RENTAL_COUNT", label: "인기" },
];

// ========================================
// 카테고리 옵션 상수
// ========================================

export interface CategoryOption {
  code: ProductCategory;
  label: string;
}

export const CATEGORY_OPTIONS: CategoryOption[] = [
  { code: "ELECTRONICS", label: "전자기기" },
  { code: "FURNITURE", label: "가구" },
  { code: "SPORTS", label: "스포츠" },
  { code: "FASHION", label: "패션" },
  { code: "BOOKS", label: "도서" },
  { code: "TOOLS", label: "공구" },
  { code: "VEHICLES", label: "이동수단" },
  { code: "OTHERS", label: "기타" },
];
