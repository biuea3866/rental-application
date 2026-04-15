import type {
  Product,
  ProductSummary,
  GuidePriceRange,
  ProductCategory,
} from "@/lib/api/types";

// ========================================
// 상품 스텁 데이터 — BE ProductDetailResponse 기준
// ========================================

export const STUB_PRODUCTS: Product[] = [
  // ELECTRONICS
  {
    id: 1,
    userId: 1,
    name: "소니 A7C II 미러리스 카메라",
    description: "풀프레임 미러리스 카메라, 렌즈 포함. 여행/촬영용으로 적합합니다.",
    categoryCode: "ELECTRONICS",
    condition: "GOOD",
    depositAmount: 500000,
    status: "APPROVED",
    prices: [{ id: 1, rentalUnit: "DAILY", priceAmount: 35000 }],
    images: [{ id: 1, objectKey: "/images/stub/camera.jpg", originalFilename: "camera.jpg", sortOrder: 0 }],
    createdAt: "2026-03-01T10:00:00Z",
    updatedAt: "2026-03-01T10:00:00Z",
  },
  {
    id: 2,
    userId: 1,
    name: "맥북 프로 16인치 M3 Pro",
    description: "개발/디자인 작업용 맥북. 충전기, 파우치 포함.",
    categoryCode: "ELECTRONICS",
    condition: "LIKE_NEW",
    depositAmount: 1000000,
    status: "APPROVED",
    prices: [{ id: 2, rentalUnit: "DAILY", priceAmount: 50000 }],
    images: [{ id: 2, objectKey: "/images/stub/macbook.jpg", originalFilename: "macbook.jpg", sortOrder: 0 }],
    createdAt: "2026-03-05T14:00:00Z",
    updatedAt: "2026-03-05T14:00:00Z",
  },
  // FURNITURE
  {
    id: 3,
    userId: 2,
    name: "허먼밀러 에어론 체어",
    description: "인체공학 사무용 의자. 풀옵션 모델.",
    categoryCode: "FURNITURE",
    condition: "GOOD",
    depositAmount: 300000,
    status: "APPROVED",
    prices: [{ id: 3, rentalUnit: "DAILY", priceAmount: 15000 }],
    images: [{ id: 3, objectKey: "/images/stub/chair.jpg", originalFilename: "chair.jpg", sortOrder: 0 }],
    createdAt: "2026-02-20T09:00:00Z",
    updatedAt: "2026-02-20T09:00:00Z",
  },
  // SPORTS
  {
    id: 4,
    userId: 1,
    name: "캠핑 텐트 4인용 (스노우피크)",
    description: "4인용 돔형 텐트. 방수 완벽, 설치 간편.",
    categoryCode: "SPORTS",
    condition: "GOOD",
    depositAmount: 200000,
    status: "APPROVED",
    prices: [{ id: 4, rentalUnit: "DAILY", priceAmount: 20000 }],
    images: [{ id: 4, objectKey: "/images/stub/tent.jpg", originalFilename: "tent.jpg", sortOrder: 0 }],
    createdAt: "2026-03-10T11:00:00Z",
    updatedAt: "2026-03-10T11:00:00Z",
  },
  {
    id: 5,
    userId: 2,
    name: "로드바이크 자이언트 TCR",
    description: "카본 프레임 로드바이크. 사이즈 M(170~180cm 적합).",
    categoryCode: "SPORTS",
    condition: "GOOD",
    depositAmount: 500000,
    status: "SUSPENDED",
    prices: [{ id: 5, rentalUnit: "DAILY", priceAmount: 25000 }],
    images: [{ id: 5, objectKey: "/images/stub/bike.jpg", originalFilename: "bike.jpg", sortOrder: 0 }],
    createdAt: "2026-02-28T16:00:00Z",
    updatedAt: "2026-03-15T09:00:00Z",
  },
  // FASHION
  {
    id: 6,
    userId: 1,
    name: "구찌 GG 마몬트 숄더백",
    description: "명품 숄더백. 파티/행사용 단기 대여 추천.",
    categoryCode: "FASHION",
    condition: "LIKE_NEW",
    depositAmount: 800000,
    status: "APPROVED",
    prices: [{ id: 6, rentalUnit: "DAILY", priceAmount: 30000 }],
    images: [{ id: 6, objectKey: "/images/stub/bag.jpg", originalFilename: "bag.jpg", sortOrder: 0 }],
    createdAt: "2026-03-12T13:00:00Z",
    updatedAt: "2026-03-12T13:00:00Z",
  },
  // BOOKS
  {
    id: 7,
    userId: 2,
    name: "토익 교재 세트 (해커스 전과목)",
    description: "토익 준비 교재 세트. 리스닝/리딩/실전모의고사 포함.",
    categoryCode: "BOOKS",
    condition: "GOOD",
    depositAmount: 30000,
    status: "APPROVED",
    prices: [{ id: 7, rentalUnit: "DAILY", priceAmount: 2000 }],
    images: [{ id: 7, objectKey: "/images/stub/books.jpg", originalFilename: "books.jpg", sortOrder: 0 }],
    createdAt: "2026-01-25T10:00:00Z",
    updatedAt: "2026-01-25T10:00:00Z",
  },
  // TOOLS
  {
    id: 8,
    userId: 1,
    name: "보쉬 전동드릴 세트",
    description: "가정용 전동드릴 + 비트 세트. DIY/인테리어에 적합.",
    categoryCode: "TOOLS",
    condition: "GOOD",
    depositAmount: 100000,
    status: "APPROVED",
    prices: [{ id: 8, rentalUnit: "DAILY", priceAmount: 8000 }],
    images: [{ id: 8, objectKey: "/images/stub/drill.jpg", originalFilename: "drill.jpg", sortOrder: 0 }],
    createdAt: "2026-03-08T15:00:00Z",
    updatedAt: "2026-03-08T15:00:00Z",
  },
  // VEHICLES
  {
    id: 9,
    userId: 2,
    name: "전동 킥보드 샤오미 프로2",
    description: "접이식 전동 킥보드. 최대 45km 주행 가능.",
    categoryCode: "VEHICLES",
    condition: "GOOD",
    depositAmount: 150000,
    status: "APPROVED",
    prices: [{ id: 9, rentalUnit: "DAILY", priceAmount: 10000 }],
    images: [{ id: 9, objectKey: "/images/stub/scooter.jpg", originalFilename: "scooter.jpg", sortOrder: 0 }],
    createdAt: "2026-03-03T12:00:00Z",
    updatedAt: "2026-03-03T12:00:00Z",
  },
  // OTHERS
  {
    id: 10,
    userId: 1,
    name: "빔프로젝터 엡손 EH-TW7100",
    description: "4K HDR 홈시네마 프로젝터. 스크린 포함.",
    categoryCode: "OTHERS",
    condition: "LIKE_NEW",
    depositAmount: 400000,
    status: "APPROVED",
    prices: [{ id: 10, rentalUnit: "DAILY", priceAmount: 25000 }],
    images: [{ id: 10, objectKey: "/images/stub/projector.jpg", originalFilename: "projector.jpg", sortOrder: 0 }],
    createdAt: "2026-03-15T10:00:00Z",
    updatedAt: "2026-03-15T10:00:00Z",
  },
];

/** ProductDetail → ProductSummary 변환 헬퍼 (목록 표시용) */
export function toProductSummary(product: Product): ProductSummary {
  return {
    id: product.id,
    name: product.name,
    categoryCode: product.categoryCode,
    status: product.status,
    depositAmount: product.depositAmount,
    thumbnailUrl: product.images[0]?.objectKey ?? null,
    createdAt: product.createdAt,
  };
}

export const STUB_PRODUCT_SUMMARIES: ProductSummary[] = STUB_PRODUCTS.map(toProductSummary);

// ========================================
// 가이드 가격 스텁 데이터
// ========================================

export const STUB_GUIDE_PRICES: GuidePriceRange[] = [
  {
    category: "ELECTRONICS",
    minPricePerDay: 10000,
    maxPricePerDay: 100000,
    averagePricePerDay: 40000,
  },
  {
    category: "FURNITURE",
    minPricePerDay: 5000,
    maxPricePerDay: 50000,
    averagePricePerDay: 15000,
  },
  {
    category: "SPORTS",
    minPricePerDay: 5000,
    maxPricePerDay: 80000,
    averagePricePerDay: 20000,
  },
  {
    category: "FASHION",
    minPricePerDay: 10000,
    maxPricePerDay: 100000,
    averagePricePerDay: 30000,
  },
  {
    category: "BOOKS",
    minPricePerDay: 500,
    maxPricePerDay: 5000,
    averagePricePerDay: 2000,
  },
  {
    category: "TOOLS",
    minPricePerDay: 3000,
    maxPricePerDay: 30000,
    averagePricePerDay: 8000,
  },
  {
    category: "VEHICLES",
    minPricePerDay: 5000,
    maxPricePerDay: 200000,
    averagePricePerDay: 30000,
  },
  {
    category: "OTHERS",
    minPricePerDay: 3000,
    maxPricePerDay: 50000,
    averagePricePerDay: 15000,
  },
];

// ========================================
// 스텁 헬퍼
// ========================================

export function findProductById(id: number | string): Product | undefined {
  return STUB_PRODUCTS.find((p) => p.id === Number(id));
}

export function findProductsByCategory(category: ProductCategory): Product[] {
  return STUB_PRODUCTS.filter((p) => p.categoryCode === category);
}

export function findGuidePriceByCategory(
  category: ProductCategory
): GuidePriceRange | undefined {
  return STUB_GUIDE_PRICES.find((g) => g.category === category);
}
