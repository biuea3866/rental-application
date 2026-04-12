import { http, HttpResponse, delay } from "msw";
import {
  findDraftById,
  createStubDraft,
  updateStubDraft,
  submitStubDraft,
  generatePresignedUrl,
  STUB_MY_PRODUCTS,
  resetDraftStore,
} from "../drafts";
import type {
  ProductDraft,
  MyProduct,
  MyProductStatus,
  PresignedUrlRequest,
} from "@/lib/api/types";

// ========================================
// Draft / Image / MyProduct MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export function resetDraftHandlerState(): void {
  resetDraftStore();
}

export const draftHandlers = [
  // ---- DRAFT 생성 ----
  http.post(`${BASE_URL}/api/v1/products/drafts`, async ({ request }) => {
    await delay(200);
    const body = (await request.json()) as Partial<ProductDraft>;
    const draft = createStubDraft(body);

    return HttpResponse.json({
      success: true,
      data: draft,
      timestamp: new Date().toISOString(),
    });
  }),

  // ---- DRAFT 수정 ----
  http.patch(
    `${BASE_URL}/api/v1/products/drafts/:id`,
    async ({ params, request }) => {
      await delay(150);
      const body = (await request.json()) as Partial<ProductDraft>;
      const updated = updateStubDraft(params.id as string, body);

      if (!updated) {
        return HttpResponse.json(
          {
            success: false,
            data: null,
            message: "드래프트를 찾을 수 없습니다.",
            timestamp: new Date().toISOString(),
          },
          { status: 404 }
        );
      }

      return HttpResponse.json({
        success: true,
        data: updated,
        timestamp: new Date().toISOString(),
      });
    }
  ),

  // ---- DRAFT 조회 ----
  http.get(`${BASE_URL}/api/v1/products/drafts/:id`, async ({ params }) => {
    await delay(150);
    const draft = findDraftById(params.id as string);

    if (!draft) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "드래프트를 찾을 수 없습니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      success: true,
      data: draft,
      timestamp: new Date().toISOString(),
    });
  }),

  // ---- DRAFT 검수 제출 ----
  http.post(
    `${BASE_URL}/api/v1/products/drafts/:id/submit`,
    async ({ params }) => {
      await delay(300);
      const submitted = submitStubDraft(params.id as string);

      if (!submitted) {
        return HttpResponse.json(
          {
            success: false,
            data: null,
            message: "드래프트를 찾을 수 없습니다.",
            timestamp: new Date().toISOString(),
          },
          { status: 404 }
        );
      }

      return HttpResponse.json({
        success: true,
        data: submitted,
        timestamp: new Date().toISOString(),
      });
    }
  ),

  // ---- Presigned URL 획득 ----
  http.post(`${BASE_URL}/api/v1/images/presigned-url`, async ({ request }) => {
    await delay(100);
    const body = (await request.json()) as PresignedUrlRequest;
    const result = generatePresignedUrl(body.fileName);

    return HttpResponse.json({
      success: true,
      data: result,
      timestamp: new Date().toISOString(),
    });
  }),

  // ---- 내 상품 목록 ----
  http.get(`${BASE_URL}/api/v1/my-products`, async ({ request }) => {
    await delay(200);
    const url = new URL(request.url);
    const status = url.searchParams.get("status");

    let products: MyProduct[] = [...STUB_MY_PRODUCTS];
    if (status) {
      products = products.filter(
        (p) => p.status === (status as MyProductStatus)
      );
    }

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
  }),
];
