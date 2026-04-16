import type {
  RentalSummary,
  RentalDetail,
  RentalStatus,
} from "@/lib/api/types";

// ========================================
// 대여 스텁 데이터 (BE TDD-002 기준)
// ========================================

export const STUB_RENTAL_SUMMARIES: RentalSummary[] = [
  {
    rentalId: 1001,
    productId: 4,
    productName: "캠핑 텐트 4인용 (스노우피크)",
    productThumbnailUrl: "/images/stub/tent.jpg",
    status: "IN_USE",
    startDate: "2026-05-01",
    endDate: "2026-05-07",
    totalAmount: 120000,
    requestedAt: "2026-04-14T10:00:00+09:00",
  },
  {
    rentalId: 1002,
    productId: 1,
    productName: "소니 A7C II 미러리스 카메라",
    productThumbnailUrl: "/images/stub/camera.jpg",
    status: "APPROVED",
    startDate: "2026-05-10",
    endDate: "2026-05-15",
    totalAmount: 175000,
    requestedAt: "2026-04-14T11:00:00+09:00",
  },
  {
    rentalId: 1003,
    productId: 2,
    productName: "맥북 프로 16인치 M3 Pro",
    productThumbnailUrl: "/images/stub/macbook.jpg",
    status: "REQUESTED",
    startDate: "2026-05-20",
    endDate: "2026-05-25",
    totalAmount: 250000,
    requestedAt: "2026-04-14T12:00:00+09:00",
  },
  {
    rentalId: 1004,
    productId: 3,
    productName: "허먼밀러 에어론 체어",
    productThumbnailUrl: "/images/stub/chair.jpg",
    status: "RETURNED",
    startDate: "2026-04-01",
    endDate: "2026-04-10",
    totalAmount: 135000,
    requestedAt: "2026-03-28T09:00:00+09:00",
  },
  {
    rentalId: 1005,
    productId: 1,
    productName: "소니 A7C II 미러리스 카메라",
    productThumbnailUrl: "/images/stub/camera.jpg",
    status: "CANCELLED",
    startDate: "2026-04-15",
    endDate: "2026-04-20",
    totalAmount: 175000,
    requestedAt: "2026-04-10T14:00:00+09:00",
  },
  {
    rentalId: 1006,
    productId: 4,
    productName: "캠핑 텐트 4인용 (스노우피크)",
    productThumbnailUrl: "/images/stub/tent.jpg",
    status: "PAID",
    startDate: "2026-05-03",
    endDate: "2026-05-06",
    totalAmount: 60000,
    requestedAt: "2026-04-13T15:00:00+09:00",
  },
];

export const STUB_RENTAL_DETAILS: RentalDetail[] = [
  {
    rentalId: 1001,
    product: {
      productId: 4,
      name: "캠핑 텐트 4인용 (스노우피크)",
      thumbnailUrl: "/images/stub/tent.jpg",
      category: "SPORTS",
    },
    renter: { userId: 3, name: "이빌림" },
    lender: { userId: 1, name: "김대여" },
    status: "IN_USE",
    startDate: "2026-05-01",
    endDate: "2026-05-07",
    totalAmount: 120000,
    depositAmount: 200000,
    deliveryInfo: {
      recipientName: "이빌림",
      recipientPhone: "010-3456-7890",
      addressLine1: "서울특별시 강남구 테헤란로 123",
      addressLine2: "101호",
      zipCode: "06234",
    },
    payment: {
      paymentId: 5001,
      amount: 120000,
      paymentMethod: "CARD",
      status: "COMPLETED",
      paidAt: "2026-04-14T12:00:00+09:00",
    },
    requestedAt: "2026-04-14T10:00:00+09:00",
    approvedAt: "2026-04-14T11:00:00+09:00",
    paidAt: "2026-04-14T12:00:00+09:00",
    startedAt: "2026-04-15T09:00:00+09:00",
  },
  {
    rentalId: 1002,
    product: {
      productId: 1,
      name: "소니 A7C II 미러리스 카메라",
      thumbnailUrl: "/images/stub/camera.jpg",
      category: "ELECTRONICS",
    },
    renter: { userId: 3, name: "이빌림" },
    lender: { userId: 1, name: "김대여" },
    status: "APPROVED",
    startDate: "2026-05-10",
    endDate: "2026-05-15",
    totalAmount: 175000,
    depositAmount: 500000,
    deliveryInfo: {
      recipientName: "이빌림",
      recipientPhone: "010-3456-7890",
      addressLine1: "서울특별시 마포구 홍대로 456",
      addressLine2: "202호",
      zipCode: "04066",
    },
    requestedAt: "2026-04-14T11:00:00+09:00",
    approvedAt: "2026-04-14T13:00:00+09:00",
  },
  {
    rentalId: 1003,
    product: {
      productId: 2,
      name: "맥북 프로 16인치 M3 Pro",
      thumbnailUrl: "/images/stub/macbook.jpg",
      category: "ELECTRONICS",
    },
    renter: { userId: 3, name: "이빌림" },
    lender: { userId: 2, name: "박렌트" },
    status: "REQUESTED",
    startDate: "2026-05-20",
    endDate: "2026-05-25",
    totalAmount: 250000,
    depositAmount: 1000000,
    deliveryInfo: {
      recipientName: "이빌림",
      recipientPhone: "010-3456-7890",
      addressLine1: "서울특별시 강남구 테헤란로 123",
      addressLine2: "101호",
      zipCode: "06234",
    },
    requestedAt: "2026-04-14T12:00:00+09:00",
  },
  {
    rentalId: 1004,
    product: {
      productId: 3,
      name: "허먼밀러 에어론 체어",
      thumbnailUrl: "/images/stub/chair.jpg",
      category: "FURNITURE",
    },
    renter: { userId: 4, name: "최구독" },
    lender: { userId: 2, name: "박렌트" },
    status: "RETURNED",
    startDate: "2026-04-01",
    endDate: "2026-04-10",
    totalAmount: 135000,
    depositAmount: 300000,
    deliveryInfo: {
      recipientName: "최구독",
      recipientPhone: "010-4567-8901",
      addressLine1: "서울특별시 서초구 반포대로 789",
      zipCode: "06540",
    },
    payment: {
      paymentId: 5002,
      amount: 135000,
      paymentMethod: "TOSS_PAY",
      status: "COMPLETED",
      paidAt: "2026-03-30T10:00:00+09:00",
    },
    requestedAt: "2026-03-28T09:00:00+09:00",
    approvedAt: "2026-03-29T10:00:00+09:00",
    paidAt: "2026-03-30T10:00:00+09:00",
    startedAt: "2026-04-01T09:00:00+09:00",
    returnedAt: "2026-04-10T18:00:00+09:00",
  },
];

// ========================================
// 스텁 헬퍼 함수
// ========================================

export function findRentalById(rentalId: number): RentalDetail | undefined {
  return STUB_RENTAL_DETAILS.find((r) => r.rentalId === rentalId);
}

export function filterRentalsByStatus(
  summaries: RentalSummary[],
  status?: RentalStatus
): RentalSummary[] {
  if (!status) return summaries;
  return summaries.filter((r) => r.status === status);
}
