import { http, HttpResponse, delay } from "msw";
import {
  STUB_PRODUCTS,
  STUB_GUIDE_PRICES,
  findProductById,
  findProductsByCategory,
  findGuidePriceByCategory,
} from "../products";
import type { ProductCategory } from "@/lib/api/types";

// ========================================
// Product MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const productHandlers = [
  // 상품 목록 조회 (검색/필터/페이지네이션 지원)
  http.get(`${BASE_URL}/api/v1/products`, async ({ request }) => {
    await delay(200);

    const url = new URL(request.url);
    const category = url.searchParams.get("category") as ProductCategory | null;
    const keyword = url.searchParams.get("keyword");
    const minPrice = url.searchParams.get("minPrice");
    const maxPrice = url.searchParams.get("maxPrice");
    const sort = url.searchParams.get("sort") ?? "latest";
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    let products = category
      ? findProductsByCategory(category)
      : [...STUB_PRODUCTS];

    // 키워드 필터
    if (keyword) {
      const q = keyword.toLowerCase();
      products = products.filter(
        (p) =>
          p.title.toLowerCase().includes(q) ||
          p.description.toLowerCase().includes(q)
      );
    }

    // 가격 필터
    if (minPrice) {
      products = products.filter((p) => p.pricePerDay >= Number(minPrice));
    }
    if (maxPrice) {
      products = products.filter((p) => p.pricePerDay <= Number(maxPrice));
    }

    // 정렬
    if (sort === "price_asc") {
      products = [...products].sort((a, b) => a.pricePerDay - b.pricePerDay);
    } else if (sort === "price_desc") {
      products = [...products].sort((a, b) => b.pricePerDay - a.pricePerDay);
    } else {
      products = [...products].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    }

    // 페이지네이션
    const totalElements = products.length;
    const totalPages = Math.ceil(totalElements / size);
    const start = page * size;
    const content = products.slice(start, start + size);

    return HttpResponse.json({
      success: true,
      data: {
        content,
        page,
        size,
        totalElements,
        totalPages,
        hasNext: page < totalPages - 1,
      },
      timestamp: new Date().toISOString(),
    });
  }),

  // 상품 상세 조회
  http.get(`${BASE_URL}/api/v1/products/:id`, async ({ params }) => {
    await delay(150);

    const product = findProductById(params.id as string);

    if (!product) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "상품을 찾을 수 없습니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      success: true,
      data: product,
      timestamp: new Date().toISOString(),
    });
  }),

  // 상품 검색
  http.get(`${BASE_URL}/api/v1/products/search`, async ({ request }) => {
    await delay(200);

    const url = new URL(request.url);
    const query = url.searchParams.get("q") || "";

    const results = STUB_PRODUCTS.filter(
      (p) =>
        p.title.includes(query) ||
        p.description.includes(query) ||
        p.category.toLowerCase().includes(query.toLowerCase())
    );

    return HttpResponse.json({
      success: true,
      data: {
        content: results,
        page: 0,
        size: 20,
        totalElements: results.length,
        totalPages: 1,
        hasNext: false,
      },
      timestamp: new Date().toISOString(),
    });
  }),

  // 내 등록 상품 조회 (등록자용)
  http.get(`${BASE_URL}/api/v1/products/mine`, async () => {
    await delay(200);

    // 등록자 001 소유 상품 반환
    const myProducts = STUB_PRODUCTS.filter(
      (p) => p.lenderId === "user-lender-001"
    );

    return HttpResponse.json({
      success: true,
      data: {
        content: myProducts,
        page: 0,
        size: 20,
        totalElements: myProducts.length,
        totalPages: 1,
        hasNext: false,
      },
      timestamp: new Date().toISOString(),
    });
  }),

  // 가이드 가격 전체 조회
  http.get(`${BASE_URL}/api/v1/guide-prices`, async () => {
    await delay(100);

    return HttpResponse.json({
      success: true,
      data: STUB_GUIDE_PRICES,
      timestamp: new Date().toISOString(),
    });
  }),

  // 카테고리별 가이드 가격 조회
  http.get(`${BASE_URL}/api/v1/guide-prices/:category`, async ({ params }) => {
    await delay(100);

    const guidePrice = findGuidePriceByCategory(
      params.category as ProductCategory
    );

    if (!guidePrice) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "해당 카테고리의 가이드 가격을 찾을 수 없습니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      success: true,
      data: guidePrice,
      timestamp: new Date().toISOString(),
    });
  }),
];
