import { http, HttpResponse, delay } from "msw";
import {
  STUB_PRODUCTS,
  STUB_PRODUCT_SUMMARIES,
  STUB_GUIDE_PRICES,
  findProductById,
  findGuidePriceByCategory,
} from "../products";
import { getDailyPrice } from "@/lib/api/types";
import type { ProductCategory, ProductSummary } from "@/lib/api/types";

// ========================================
// Product MSW 핸들러 (BE 스키마 기준)
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const productHandlers = [
  // 상품 목록 조회 (BE: GET /api/v1/products — Spring Page<ProductSummaryResponse>)
  http.get(`${BASE_URL}/api/v1/products`, async ({ request }) => {
    await delay(200);

    const url = new URL(request.url);
    const category = url.searchParams.get("category") as ProductCategory | null;
    const keyword = url.searchParams.get("keyword");
    const minPrice = url.searchParams.get("minPrice");
    const maxPrice = url.searchParams.get("maxPrice");
    const sortBy = url.searchParams.get("sortBy") ?? "CREATED_AT";
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    let summaries = category
      ? STUB_PRODUCT_SUMMARIES.filter((p) => p.categoryCode === category)
      : [...STUB_PRODUCT_SUMMARIES];

    // 키워드 필터 (name 기준)
    if (keyword) {
      const q = keyword.toLowerCase();
      summaries = summaries.filter(
        (p) => p.name?.toLowerCase().includes(q) ?? false
      );
    }

    // 가격 필터 (STUB_PRODUCTS의 prices[] 참조)
    if (minPrice || maxPrice) {
      summaries = summaries.filter((summary) => {
        const product = STUB_PRODUCTS.find((p) => p.id === summary.id);
        if (!product) return true;
        const dailyPrice = getDailyPrice(product);
        if (dailyPrice == null) return true;
        if (minPrice && dailyPrice < Number(minPrice)) return false;
        if (maxPrice && dailyPrice > Number(maxPrice)) return false;
        return true;
      });
    }

    // 정렬
    if (sortBy === "PRICE_ASC") {
      summaries = [...summaries].sort((a, b) => {
        const pa = STUB_PRODUCTS.find((p) => p.id === a.id);
        const pb = STUB_PRODUCTS.find((p) => p.id === b.id);
        return (getDailyPrice(pa!) ?? 0) - (getDailyPrice(pb!) ?? 0);
      });
    } else if (sortBy === "PRICE_DESC") {
      summaries = [...summaries].sort((a, b) => {
        const pa = STUB_PRODUCTS.find((p) => p.id === a.id);
        const pb = STUB_PRODUCTS.find((p) => p.id === b.id);
        return (getDailyPrice(pb!) ?? 0) - (getDailyPrice(pa!) ?? 0);
      });
    } else {
      summaries = [...summaries].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    }

    // 페이지네이션
    const totalElements = summaries.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const start = page * size;
    const content = summaries.slice(start, start + size);
    const last = page >= totalPages - 1;

    // BE는 Spring Page 구조로 반환 (data 래퍼 없음)
    return HttpResponse.json({
      content,
      number: page,
      size,
      totalElements,
      totalPages,
      last,
    });
  }),

  // 상품 상세 조회 (BE: GET /api/v1/products/:id — ProductDetailResponse)
  http.get(`${BASE_URL}/api/v1/products/:id`, async ({ params }) => {
    await delay(150);

    const product = findProductById(params.id as string);

    if (!product) {
      return HttpResponse.json(
        { code: "PRODUCT_NOT_FOUND", message: "상품을 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    return HttpResponse.json(product);
  }),

  // 내 등록 상품 조회 (BE: GET /api/v1/my-products — Spring Page<ProductSummaryResponse>)
  http.get(`${BASE_URL}/api/v1/my-products`, async () => {
    await delay(200);

    // userId=1 소유 상품 반환
    const myProducts = STUB_PRODUCTS
      .filter((p) => p.userId === 1)
      .map<ProductSummary>((p) => ({
        id: p.id,
        name: p.name,
        categoryCode: p.categoryCode,
        status: p.status,
        depositAmount: p.depositAmount,
        thumbnailUrl: p.images[0]?.objectKey ?? null,
        createdAt: p.createdAt,
      }));

    return HttpResponse.json({
      content: myProducts,
      number: 0,
      size: 20,
      totalElements: myProducts.length,
      totalPages: 1,
      last: true,
    });
  }),

  // 가이드 가격 전체 조회 (BE: GET /api/v1/price-guides)
  http.get(`${BASE_URL}/api/v1/price-guides`, async () => {
    await delay(100);

    return HttpResponse.json(STUB_GUIDE_PRICES);
  }),

  // 카테고리별 가이드 가격 조회 (BE: GET /api/v1/price-guides/:category)
  http.get(`${BASE_URL}/api/v1/price-guides/:category`, async ({ params }) => {
    await delay(100);

    const guidePrice = findGuidePriceByCategory(
      params.category as ProductCategory
    );

    if (!guidePrice) {
      return HttpResponse.json(
        { code: "NOT_FOUND", message: "해당 카테고리의 가이드 가격을 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    return HttpResponse.json(guidePrice);
  }),
];
