import { http, HttpResponse, delay } from "msw";
import type {
  DisputeResult,
  DisputeStatus,
  OpenDisputeRequest,
  AdminResolveDisputeRequest,
} from "@/lib/api/types";

// ========================================
// Dispute MSW 핸들러 (FE-450 / FE-451)
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ========================================
// 스텁 데이터
// ========================================

export const STUB_DISPUTES: DisputeResult[] = [
  {
    id: 101,
    rentalId: 1001,
    openerId: 11,
    reason: "DAMAGED",
    description: "물건을 돌려받았는데 파손 상태가 심각합니다.",
    status: "OPEN",
    attachmentUrls: ["https://example.com/img1.jpg"],
    createdAt: "2026-04-20T10:00:00+09:00",
  },
  {
    id: 102,
    rentalId: 1002,
    openerId: 11,
    reason: "LATE_RETURN",
    description: "반납 기한보다 5일 늦게 반납되었습니다.",
    status: "UNDER_REVIEW",
    attachmentUrls: [],
    createdAt: "2026-04-18T09:00:00+09:00",
  },
  {
    id: 103,
    rentalId: 1003,
    openerId: 44,
    reason: "WRONG_ITEM",
    description: "신청한 상품이 아닌 다른 상품이 왔습니다.",
    status: "RESOLVED_REFUND",
    attachmentUrls: [
      "https://example.com/img2.jpg",
      "https://example.com/img3.jpg",
    ],
    refundAmount: 30000,
    resolvedAt: "2026-04-19T15:00:00+09:00",
    createdAt: "2026-04-17T11:00:00+09:00",
  },
];

/** 핸들러 간 공유 상태 */
let stubDisputes: DisputeResult[] = STUB_DISPUTES.map((d) => ({ ...d }));
let nextDisputeId = 200;

/** 활성 분쟁 여부 판단 (OPEN | UNDER_REVIEW) */
function hasActiveDispute(rentalId: number): boolean {
  return stubDisputes.some(
    (d) =>
      d.rentalId === rentalId &&
      (d.status === "OPEN" || d.status === "UNDER_REVIEW")
  );
}

export function resetDisputeHandlerState(): void {
  // 깊은 복사로 상태 완전 초기화
  stubDisputes = STUB_DISPUTES.map((d) => ({ ...d }));
  nextDisputeId = 200;
}

// ========================================
// MSW 핸들러
// ========================================

export const disputeHandlers = [
  // ─── 당사자용 ───────────────────────────────────────────────

  // 분쟁 오픈 — POST /api/v1/disputes
  http.post(`${BASE_URL}/api/v1/disputes`, async ({ request }) => {
    await delay(200);
    const body = (await request.json()) as OpenDisputeRequest;

    if (hasActiveDispute(body.rentalId)) {
      return HttpResponse.json(
        {
          code: "DISPUTE_ALREADY_ACTIVE",
          message: "이미 진행 중인 분쟁이 있습니다.",
        },
        { status: 409 }
      );
    }

    const newDispute: DisputeResult = {
      id: nextDisputeId++,
      rentalId: body.rentalId,
      openerId: 11,
      reason: body.reason,
      description: body.description,
      status: "OPEN",
      attachmentUrls: body.attachmentUrls,
      createdAt: new Date().toISOString(),
    };
    stubDisputes.push(newDispute);
    return HttpResponse.json(newDispute, { status: 201 });
  }),

  // 내 분쟁 목록 조회 — GET /api/v1/disputes
  http.get(`${BASE_URL}/api/v1/disputes`, async ({ request }) => {
    await delay(150);
    const url = new URL(request.url);
    const statusFilter = url.searchParams.get("status") as DisputeStatus | null;
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);

    // opener=11 기준 필터 (stub)
    let filtered = stubDisputes.filter((d) => d.openerId === 11);
    if (statusFilter) {
      filtered = filtered.filter((d) => d.status === statusFilter);
    }

    const totalElements = filtered.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const content = filtered.slice(page * size, page * size + size);

    return HttpResponse.json({
      content,
      totalElements,
      totalPages,
      last: page >= totalPages - 1,
    });
  }),

  // 분쟁 상세 조회 — GET /api/v1/disputes/:id
  http.get(`${BASE_URL}/api/v1/disputes/:id`, async ({ params }) => {
    await delay(150);
    const id = Number(params.id);
    const dispute = stubDisputes.find((d) => d.id === id);
    if (!dispute) {
      return HttpResponse.json(
        { code: "DISPUTE_NOT_FOUND", message: "분쟁을 찾을 수 없습니다." },
        { status: 404 }
      );
    }
    // opener !== current user → 403 (stub에서 openerId !== 11 이면 403)
    if (dispute.openerId !== 11) {
      return HttpResponse.json(
        { code: "FORBIDDEN", message: "접근 권한이 없습니다." },
        { status: 403 }
      );
    }
    return HttpResponse.json(dispute);
  }),

  // 분쟁 취소 — POST /api/v1/disputes/:id/cancel
  http.post(
    `${BASE_URL}/api/v1/disputes/:id/cancel`,
    async ({ params }) => {
      await delay(200);
      const id = Number(params.id);
      const dispute = stubDisputes.find((d) => d.id === id);
      if (!dispute) {
        return HttpResponse.json(
          { code: "DISPUTE_NOT_FOUND", message: "분쟁을 찾을 수 없습니다." },
          { status: 404 }
        );
      }
      if (dispute.status !== "OPEN") {
        return HttpResponse.json(
          {
            code: "INVALID_STATUS",
            message: "OPEN 상태의 분쟁만 취소할 수 있습니다.",
          },
          { status: 400 }
        );
      }
      dispute.status = "CANCELLED";
      return HttpResponse.json(dispute);
    }
  ),

  // ─── 관리자용 ────────────────────────────────────────────────

  // 관리자 분쟁 목록 — GET /api/v1/admin/disputes
  http.get(`${BASE_URL}/api/v1/admin/disputes`, async ({ request }) => {
    await delay(150);
    const url = new URL(request.url);
    const statusFilter = url.searchParams.get("status") as DisputeStatus | null;
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = parseInt(url.searchParams.get("size") ?? "20", 10);
    const sort = url.searchParams.get("sort") ?? "createdAt,desc";

    let filtered = [...stubDisputes];
    if (statusFilter) {
      filtered = filtered.filter((d) => d.status === statusFilter);
    }

    if (sort.startsWith("createdAt,asc")) {
      filtered.sort(
        (a, b) =>
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      );
    } else {
      filtered.sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    }

    const totalElements = filtered.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const content = filtered.slice(page * size, page * size + size);

    return HttpResponse.json({
      content,
      totalElements,
      totalPages,
      last: page >= totalPages - 1,
    });
  }),

  // 관리자 분쟁 검토 시작 — PATCH /api/v1/admin/disputes/:id/review
  http.patch(
    `${BASE_URL}/api/v1/admin/disputes/:id/review`,
    async ({ params }) => {
      await delay(200);
      const id = Number(params.id);
      const dispute = stubDisputes.find((d) => d.id === id);
      if (!dispute) {
        return HttpResponse.json(
          { code: "DISPUTE_NOT_FOUND", message: "분쟁을 찾을 수 없습니다." },
          { status: 404 }
        );
      }
      if (dispute.status !== "OPEN") {
        return HttpResponse.json(
          {
            code: "INVALID_STATUS",
            message: "OPEN 상태의 분쟁만 검토 시작 가능합니다.",
          },
          { status: 400 }
        );
      }
      dispute.status = "UNDER_REVIEW";
      return HttpResponse.json(dispute);
    }
  ),

  // 관리자 분쟁 해결 — POST /api/v1/admin/disputes/:id/resolve
  http.post(
    `${BASE_URL}/api/v1/admin/disputes/:id/resolve`,
    async ({ params, request }) => {
      await delay(250);
      const id = Number(params.id);
      const body = (await request.json()) as AdminResolveDisputeRequest;
      const dispute = stubDisputes.find((d) => d.id === id);
      if (!dispute) {
        return HttpResponse.json(
          { code: "DISPUTE_NOT_FOUND", message: "분쟁을 찾을 수 없습니다." },
          { status: 404 }
        );
      }
      if (dispute.status !== "UNDER_REVIEW") {
        return HttpResponse.json(
          {
            code: "INVALID_STATUS",
            message: "UNDER_REVIEW 상태의 분쟁만 해결 가능합니다.",
          },
          { status: 400 }
        );
      }

      if (body.type === "FULL_REFUND") {
        dispute.status = "RESOLVED_REFUND";
      } else if (body.type === "PARTIAL") {
        dispute.status = "RESOLVED_PARTIAL";
        dispute.refundAmount = body.refundAmount;
      } else {
        dispute.status = "RESOLVED_REJECTED";
      }
      dispute.resolvedAt = new Date().toISOString();
      return HttpResponse.json(dispute);
    }
  ),
];
