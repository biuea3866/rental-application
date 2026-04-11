import type { Product, GuidePriceRange, ProductCategory } from "@/lib/api/types";

// ========================================
// 상품 스텁 데이터 (카테고리별 10개)
// ========================================

export const STUB_PRODUCTS: Product[] = [
  // ELECTRONICS
  {
    id: "prod-001",
    title: "소니 A7C II 미러리스 카메라",
    description: "풀프레임 미러리스 카메라, 렌즈 포함. 여행/촬영용으로 적합합니다.",
    category: "ELECTRONICS",
    pricePerDay: 35000,
    deposit: 500000,
    imageUrls: ["/images/stub/camera.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-001",
    lenderName: "김대여",
    location: "서울 강남구",
    createdAt: "2026-03-01T10:00:00Z",
    updatedAt: "2026-03-01T10:00:00Z",
  },
  {
    id: "prod-002",
    title: "맥북 프로 16인치 M3 Pro",
    description: "개발/디자인 작업용 맥북. 충전기, 파우치 포함.",
    category: "ELECTRONICS",
    pricePerDay: 50000,
    deposit: 1000000,
    imageUrls: ["/images/stub/macbook.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-001",
    lenderName: "김대여",
    location: "서울 서초구",
    createdAt: "2026-03-05T14:00:00Z",
    updatedAt: "2026-03-05T14:00:00Z",
  },
  // FURNITURE
  {
    id: "prod-003",
    title: "허먼밀러 에어론 체어",
    description: "인체공학 사무용 의자. 풀옵션 모델.",
    category: "FURNITURE",
    pricePerDay: 15000,
    deposit: 300000,
    imageUrls: ["/images/stub/chair.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-002",
    lenderName: "박렌트",
    location: "서울 마포구",
    createdAt: "2026-02-20T09:00:00Z",
    updatedAt: "2026-02-20T09:00:00Z",
  },
  // SPORTS
  {
    id: "prod-004",
    title: "캠핑 텐트 4인용 (스노우피크)",
    description: "4인용 돔형 텐트. 방수 완벽, 설치 간편.",
    category: "SPORTS",
    pricePerDay: 20000,
    deposit: 200000,
    imageUrls: ["/images/stub/tent.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-001",
    lenderName: "김대여",
    location: "경기 성남시",
    createdAt: "2026-03-10T11:00:00Z",
    updatedAt: "2026-03-10T11:00:00Z",
  },
  {
    id: "prod-005",
    title: "로드바이크 자이언트 TCR",
    description: "카본 프레임 로드바이크. 사이즈 M(170~180cm 적합).",
    category: "SPORTS",
    pricePerDay: 25000,
    deposit: 500000,
    imageUrls: ["/images/stub/bike.jpg"],
    status: "RENTED",
    lenderId: "user-lender-002",
    lenderName: "박렌트",
    location: "서울 송파구",
    createdAt: "2026-02-28T16:00:00Z",
    updatedAt: "2026-03-15T09:00:00Z",
  },
  // FASHION
  {
    id: "prod-006",
    title: "구찌 GG 마몬트 숄더백",
    description: "명품 숄더백. 파티/행사용 단기 대여 추천.",
    category: "FASHION",
    pricePerDay: 30000,
    deposit: 800000,
    imageUrls: ["/images/stub/bag.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-001",
    lenderName: "김대여",
    location: "서울 강남구",
    createdAt: "2026-03-12T13:00:00Z",
    updatedAt: "2026-03-12T13:00:00Z",
  },
  // BOOKS
  {
    id: "prod-007",
    title: "토익 교재 세트 (해커스 전과목)",
    description: "토익 준비 교재 세트. 리스닝/리딩/실전모의고사 포함.",
    category: "BOOKS",
    pricePerDay: 2000,
    deposit: 30000,
    imageUrls: ["/images/stub/books.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-002",
    lenderName: "박렌트",
    location: "서울 관악구",
    createdAt: "2026-01-25T10:00:00Z",
    updatedAt: "2026-01-25T10:00:00Z",
  },
  // TOOLS
  {
    id: "prod-008",
    title: "보쉬 전동드릴 세트",
    description: "가정용 전동드릴 + 비트 세트. DIY/인테리어에 적합.",
    category: "TOOLS",
    pricePerDay: 8000,
    deposit: 100000,
    imageUrls: ["/images/stub/drill.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-001",
    lenderName: "김대여",
    location: "서울 용산구",
    createdAt: "2026-03-08T15:00:00Z",
    updatedAt: "2026-03-08T15:00:00Z",
  },
  // VEHICLES
  {
    id: "prod-009",
    title: "전동 킥보드 샤오미 프로2",
    description: "접이식 전동 킥보드. 최대 45km 주행 가능.",
    category: "VEHICLES",
    pricePerDay: 10000,
    deposit: 150000,
    imageUrls: ["/images/stub/scooter.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-002",
    lenderName: "박렌트",
    location: "서울 영등포구",
    createdAt: "2026-03-03T12:00:00Z",
    updatedAt: "2026-03-03T12:00:00Z",
  },
  // OTHERS
  {
    id: "prod-010",
    title: "빔프로젝터 엡손 EH-TW7100",
    description: "4K HDR 홈시네마 프로젝터. 스크린 포함.",
    category: "OTHERS",
    pricePerDay: 25000,
    deposit: 400000,
    imageUrls: ["/images/stub/projector.jpg"],
    status: "AVAILABLE",
    lenderId: "user-lender-001",
    lenderName: "김대여",
    location: "서울 강서구",
    createdAt: "2026-03-15T10:00:00Z",
    updatedAt: "2026-03-15T10:00:00Z",
  },
];

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

export function findProductById(id: string): Product | undefined {
  return STUB_PRODUCTS.find((p) => p.id === id);
}

export function findProductsByCategory(category: ProductCategory): Product[] {
  return STUB_PRODUCTS.filter((p) => p.category === category);
}

export function findGuidePriceByCategory(
  category: ProductCategory
): GuidePriceRange | undefined {
  return STUB_GUIDE_PRICES.find((g) => g.category === category);
}
