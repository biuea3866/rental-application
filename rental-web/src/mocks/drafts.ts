import type {
  ProductDraft,
  MyProduct,
  PresignedUrlResponse,
} from "@/lib/api/types";

// ========================================
// 드래프트 스텁 데이터
// ========================================

export const STUB_DRAFTS: ProductDraft[] = [
  {
    id: "draft-001",
    title: "니콘 Z5 미러리스 카메라",
    description: "입문용 미러리스 카메라. 단렌즈 포함.",
    category: "ELECTRONICS",
    deposit: 300000,
    pricePerDay: 25000,
    pricePerWeek: 150000,
    pricePerMonth: 500000,
    imageUrls: ["/images/stub/nikon.jpg"],
    condition: "LIKE_NEW",
    conditionNote: "구매 후 3회 사용. 흠집 없음.",
    location: "서울 강남구",
    status: "DRAFT",
    lenderId: "user-lender-001",
    createdAt: "2026-04-01T10:00:00Z",
    updatedAt: "2026-04-01T10:00:00Z",
  },
  {
    id: "draft-002",
    title: "캠핑 코펠 세트",
    description: "2인용 캠핑 코펠 세트. 버너 포함.",
    category: "SPORTS",
    deposit: 50000,
    pricePerDay: 5000,
    pricePerWeek: 30000,
    pricePerMonth: 100000,
    imageUrls: [],
    condition: "GOOD",
    conditionNote: "사용감 있으나 기능 정상.",
    location: "서울 마포구",
    status: "SUBMITTED",
    lenderId: "user-lender-001",
    createdAt: "2026-04-05T14:00:00Z",
    updatedAt: "2026-04-06T09:00:00Z",
  },
];

// ========================================
// 내 상품 목록 스텁 데이터
// ========================================

export const STUB_MY_PRODUCTS: MyProduct[] = [
  {
    id: "my-prod-001",
    title: "소니 A7C II 미러리스 카메라",
    description: "풀프레임 미러리스 카메라, 렌즈 포함.",
    category: "ELECTRONICS",
    pricePerDay: 35000,
    deposit: 500000,
    imageUrls: ["/images/stub/camera.jpg"],
    status: "AVAILABLE",
    location: "서울 강남구",
    createdAt: "2026-03-01T10:00:00Z",
    updatedAt: "2026-03-01T10:00:00Z",
  },
  {
    id: "my-prod-002",
    title: "허먼밀러 에어론 체어",
    description: "인체공학 사무용 의자. 풀옵션 모델.",
    category: "FURNITURE",
    pricePerDay: 15000,
    deposit: 300000,
    imageUrls: ["/images/stub/chair.jpg"],
    status: "RENTED",
    location: "서울 마포구",
    createdAt: "2026-02-20T09:00:00Z",
    updatedAt: "2026-02-20T09:00:00Z",
  },
  {
    id: "draft-001",
    title: "니콘 Z5 미러리스 카메라",
    description: "입문용 미러리스 카메라. 단렌즈 포함.",
    category: "ELECTRONICS",
    pricePerDay: 25000,
    deposit: 300000,
    imageUrls: [],
    status: "DRAFT",
    location: "서울 강남구",
    createdAt: "2026-04-01T10:00:00Z",
    updatedAt: "2026-04-01T10:00:00Z",
  },
  {
    id: "draft-002",
    title: "캠핑 코펠 세트",
    description: "2인용 캠핑 코펠 세트. 버너 포함.",
    category: "SPORTS",
    pricePerDay: 5000,
    deposit: 50000,
    imageUrls: [],
    status: "SUBMITTED",
    location: "서울 마포구",
    createdAt: "2026-04-05T14:00:00Z",
    updatedAt: "2026-04-06T09:00:00Z",
  },
];

// ========================================
// Presigned URL 스텁
// ========================================

export function generatePresignedUrl(fileName: string): PresignedUrlResponse {
  const fileKey = `uploads/${Date.now()}-${fileName}`;
  return {
    presignedUrl: `https://stub-s3.example.com/${fileKey}?presigned=true`,
    fileUrl: `https://cdn.example.com/${fileKey}`,
    expiresIn: 3600,
  };
}

// ========================================
// 스텁 헬퍼
// ========================================

let draftStore: ProductDraft[] = [...STUB_DRAFTS];

export function resetDraftStore(): void {
  draftStore = [...STUB_DRAFTS];
}

export function findDraftById(id: string): ProductDraft | undefined {
  return draftStore.find((d) => d.id === id);
}

export function createStubDraft(
  data: Partial<ProductDraft>
): ProductDraft {
  const draft: ProductDraft = {
    id: `draft-${Date.now()}`,
    title: data.title ?? "",
    description: data.description ?? "",
    category: data.category ?? "OTHERS",
    deposit: data.deposit ?? 0,
    pricePerDay: data.pricePerDay ?? 0,
    pricePerWeek: data.pricePerWeek,
    pricePerMonth: data.pricePerMonth,
    imageUrls: data.imageUrls ?? [],
    condition: data.condition ?? "GOOD",
    conditionNote: data.conditionNote ?? "",
    location: data.location ?? "",
    status: "DRAFT",
    lenderId: "user-lender-001",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  draftStore.push(draft);
  return draft;
}

export function updateStubDraft(
  id: string,
  data: Partial<ProductDraft>
): ProductDraft | undefined {
  const idx = draftStore.findIndex((d) => d.id === id);
  if (idx === -1) return undefined;
  draftStore[idx] = {
    ...draftStore[idx],
    ...data,
    updatedAt: new Date().toISOString(),
  };
  return draftStore[idx];
}

export function submitStubDraft(
  id: string
): ProductDraft | undefined {
  return updateStubDraft(id, { status: "SUBMITTED" });
}
