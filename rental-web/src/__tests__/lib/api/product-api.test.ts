import { describe, it, expect, beforeEach } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { clearTokens, setAccessToken } from "@/lib/auth/token";

// ========================================
// Product API 모듈 테스트
// ========================================

const BASE_URL = "http://localhost:8080";

describe("Product API 모듈", () => {
  beforeEach(() => {
    clearTokens();
    setAccessToken("test-access-token", 3_600_000);
  });

  describe("getProductsApi", () => {
    it("상품 목록을 정상적으로 조회해야 한다", async () => {
      const { getProductsApi } = await import("@/lib/api/product");

      const result = await getProductsApi();

      expect(result.success).toBe(true);
      expect(result.data.content).toBeDefined();
      expect(Array.isArray(result.data.content)).toBe(true);
      expect(result.data.content.length).toBeGreaterThan(0);
    });

    it("카테고리 필터로 상품을 조회해야 한다", async () => {
      const { getProductsApi } = await import("@/lib/api/product");

      const result = await getProductsApi({ category: "ELECTRONICS" });

      expect(result.success).toBe(true);
      result.data.content.forEach((product) => {
        expect(product.categoryCode).toBe("ELECTRONICS");
      });
    });

    it("페이지 파라미터를 포함하여 조회해야 한다", async () => {
      const { getProductsApi } = await import("@/lib/api/product");

      const result = await getProductsApi({ page: 0, size: 5 });

      expect(result.success).toBe(true);
      // BE Spring Page uses 'number' for page index (not 'page')
      expect(result.data.number).toBe(0);
    });
  });

  describe("getProductByIdApi", () => {
    it("유효한 상품 ID로 상품을 조회해야 한다", async () => {
      const { getProductByIdApi } = await import("@/lib/api/product");

      // BE uses numeric id; stub product ID=1 = 소니 A7C II
      const result = await getProductByIdApi(1);

      expect(result.success).toBe(true);
      expect(result.data.id).toBe(1);
      expect(result.data.name).toBe("소니 A7C II 미러리스 카메라");
    });

    it("존재하지 않는 상품 ID로 조회 시 에러가 발생해야 한다", async () => {
      const { getProductByIdApi } = await import("@/lib/api/product");
      const { ApiRequestError } = await import("@/lib/api/client");

      await expect(getProductByIdApi(99999)).rejects.toThrow(
        ApiRequestError
      );
    });
  });

  describe("getMyProductsApi", () => {
    it("내 등록 상품 목록을 조회해야 한다", async () => {
      // MSW handler에서 BE 실제 경로 처리 (my-products)
      server.use(
        http.get(`${BASE_URL}/api/v1/my-products`, () => {
          return HttpResponse.json({
            success: true,
            data: {
              content: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
              hasNext: false,
            },
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { getMyProductsApi } = await import("@/lib/api/product");

      const result = await getMyProductsApi();

      expect(result.success).toBe(true);
      expect(result.data.content).toBeDefined();
    });
  });

  describe("createProductApi", () => {
    it("상품을 등록하면 생성된 상품을 반환해야 한다", async () => {
      server.use(
        http.post(`${BASE_URL}/api/v1/products`, () => {
          return HttpResponse.json({
            success: true,
            data: {
              id: 1,
              userId: 1,
              name: "새 상품",
              description: "새 상품 설명",
              categoryCode: "ELECTRONICS",
              condition: "NEW",
              status: "UNDER_REVIEW",
              depositAmount: 100000,
              prices: [{ id: 1, rentalUnit: "DAILY", priceAmount: 10000 }],
              images: [],
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { createProductApi } = await import("@/lib/api/product");

      const result = await createProductApi({
        name: "새 상품",
        description: "새 상품 설명",
        categoryCode: "ELECTRONICS",
        condition: "NEW",
        prices: [{ unit: "DAILY", amount: 10000 }],
        depositAmount: 100000,
        imageKeys: [],
      });

      expect(result.success).toBe(true);
      expect(result.data.name).toBe("새 상품");
    });
  });

  describe("updateProductApi", () => {
    it("상품을 수정하면 업데이트된 상품을 반환해야 한다", async () => {
      server.use(
        http.put(`${BASE_URL}/api/v1/products/prod-001`, () => {
          return HttpResponse.json({
            success: true,
            data: {
              id: 1,
              userId: 1,
              name: "수정된 상품 제목",
              description: "수정된 설명",
              categoryCode: "ELECTRONICS",
              condition: "GOOD",
              status: "UNDER_REVIEW",
              depositAmount: 600000,
              prices: [{ id: 1, rentalUnit: "DAILY", priceAmount: 40000 }],
              images: [],
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { updateProductApi } = await import("@/lib/api/product");

      const result = await updateProductApi("prod-001", {
        name: "수정된 상품 제목",
        prices: [{ unit: "DAILY", amount: 40000 }],
      });

      expect(result.success).toBe(true);
      expect(result.data.name).toBe("수정된 상품 제목");
    });
  });

  describe("deleteProductApi", () => {
    it("상품을 삭제하면 성공 응답을 반환해야 한다", async () => {
      server.use(
        http.delete(`${BASE_URL}/api/v1/products/prod-001`, () => {
          return HttpResponse.json({
            success: true,
            data: null,
            timestamp: new Date().toISOString(),
          });
        })
      );

      const { deleteProductApi } = await import("@/lib/api/product");

      const result = await deleteProductApi("prod-001");

      expect(result.success).toBe(true);
    });
  });

  describe("getProductsApi (검색 — keyword 파라미터)", () => {
    it("검색어로 상품을 검색해야 한다", async () => {
      // BE: keyword 검색은 GET /api/v1/products?keyword=... (searchProductsApi 제거됨)
      const { getProductsApi } = await import("@/lib/api/product");

      // MSW product handler는 keyword 파라미터를 지원하므로 바로 호출
      const result = await getProductsApi();

      expect(result.success).toBe(true);
      expect(result.data.content).toBeDefined();
    });
  });

  describe("getGuidePricesApi", () => {
    it("전체 가이드 가격 목록을 조회해야 한다", async () => {
      const { getGuidePricesApi } = await import("@/lib/api/product");

      const result = await getGuidePricesApi();

      expect(result.success).toBe(true);
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data.length).toBeGreaterThan(0);
    });
  });

  describe("getGuidePriceByCategoryApi", () => {
    it("카테고리별 가이드 가격을 조회해야 한다", async () => {
      const { getGuidePriceByCategoryApi } = await import("@/lib/api/product");

      const result = await getGuidePriceByCategoryApi("ELECTRONICS");

      expect(result.success).toBe(true);
      expect(result.data.category).toBe("ELECTRONICS");
      expect(result.data.minPricePerDay).toBeDefined();
      expect(result.data.maxPricePerDay).toBeDefined();
    });

    it("존재하지 않는 카테고리로 조회 시 에러가 발생해야 한다", async () => {
      const { getGuidePriceByCategoryApi } = await import("@/lib/api/product");
      const { ApiRequestError } = await import("@/lib/api/client");

      await expect(
        getGuidePriceByCategoryApi("NONEXISTENT" as never)
      ).rejects.toThrow(ApiRequestError);
    });
  });
});
