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
  // 상품 목록 조회
  http.get(`${BASE_URL}/api/v1/products`, async ({ request }) => {
    await delay(200);

    const url = new URL(request.url);
    const category = url.searchParams.get("category");

    if (category) {
      const products = findProductsByCategory(category as ProductCategory);
      return HttpResponse.json({
        success: true,
        data: {
          content: products,
          page: 0,
          size: 20,
          totalElements: products.length,
          totalPages: 1,
          hasNext: false,
        },
        timestamp: new Date().toISOString(),
      });
    }

    return HttpResponse.json({
      success: true,
      data: {
        content: STUB_PRODUCTS,
        page: 0,
        size: 20,
        totalElements: STUB_PRODUCTS.length,
        totalPages: 1,
        hasNext: false,
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
