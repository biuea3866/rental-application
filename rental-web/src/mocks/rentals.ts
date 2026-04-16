import type { Rental, RentalStatus } from "@/lib/api/types";

// ========================================
// 대여 스텁 데이터
// ========================================

let nextRentalId = 100;

export function generateRentalId(): number {
  return nextRentalId++;
}

export const STUB_RENTALS: Rental[] = [
  {
    id: 1,
    productId: 1,
    productName: "소니 A7C II 미러리스 카메라",
    productImageUrl: "/images/stub/camera.jpg",
    renterId: 2,
    renterName: "홍길동",
    lenderId: 1,
    lenderName: "김등록",
    status: "REQUESTED",
    startDate: "2026-04-20",
    endDate: "2026-04-25",
    dailyPrice: 35000,
    depositAmount: 500000,
    totalAmount: 175000 + 500000,
    deliveryInfo: {
      recipientName: "홍길동",
      recipientPhone: "010-1234-5678",
      address: "서울시 강남구 테헤란로 123",
      addressDetail: "101호",
      zipCode: "06234",
    },
    requestedAt: "2026-04-14T10:00:00Z",
  },
  {
    id: 2,
    productId: 2,
    productName: "맥북 프로 16인치 M3 Pro",
    productImageUrl: "/images/stub/macbook.jpg",
    renterId: 2,
    renterName: "홍길동",
    lenderId: 1,
    lenderName: "김등록",
    status: "APPROVED",
    startDate: "2026-04-18",
    endDate: "2026-04-21",
    dailyPrice: 50000,
    depositAmount: 1000000,
    totalAmount: 150000 + 1000000,
    deliveryInfo: {
      recipientName: "홍길동",
      recipientPhone: "010-1234-5678",
      address: "서울시 강남구 테헤란로 123",
      addressDetail: "101호",
      zipCode: "06234",
    },
    requestedAt: "2026-04-10T09:00:00Z",
    approvedAt: "2026-04-11T14:00:00Z",
  },
  {
    id: 3,
    productId: 3,
    productName: "허먼밀러 에어론 체어",
    productImageUrl: "/images/stub/chair.jpg",
    renterId: 2,
    renterName: "홍길동",
    lenderId: 1,
    lenderName: "김등록",
    status: "PAID",
    startDate: "2026-04-15",
    endDate: "2026-04-22",
    dailyPrice: 15000,
    depositAmount: 300000,
    totalAmount: 105000 + 300000,
    deliveryInfo: {
      recipientName: "홍길동",
      recipientPhone: "010-1234-5678",
      address: "서울시 강남구 테헤란로 123",
      addressDetail: "101호",
      zipCode: "06234",
    },
    payment: {
      id: 1,
      paymentKey: "toss-pay-key-abc123",
      orderId: `RC-3-${Date.now()}`,
      amount: 405000,
      status: "COMPLETED",
      paymentMethod: "카드",
      paidAt: "2026-04-12T10:30:00Z",
    },
    requestedAt: "2026-04-09T11:00:00Z",
    approvedAt: "2026-04-10T09:00:00Z",
    paidAt: "2026-04-12T10:30:00Z",
  },
  {
    id: 4,
    productId: 4,
    productName: "캠핑 텐트 4인용 (스노우피크)",
    productImageUrl: "/images/stub/tent.jpg",
    renterId: 2,
    renterName: "홍길동",
    lenderId: 1,
    lenderName: "김등록",
    status: "IN_USE",
    startDate: "2026-04-01",
    endDate: "2026-04-10",
    dailyPrice: 20000,
    depositAmount: 200000,
    totalAmount: 180000 + 200000,
    deliveryInfo: {
      recipientName: "홍길동",
      recipientPhone: "010-1234-5678",
      address: "서울시 강남구 테헤란로 123",
      addressDetail: "101호",
      zipCode: "06234",
    },
    payment: {
      id: 2,
      paymentKey: "toss-pay-key-def456",
      orderId: `RC-4-${Date.now()}`,
      amount: 380000,
      status: "COMPLETED",
      paymentMethod: "계좌이체",
      paidAt: "2026-03-30T15:00:00Z",
    },
    requestedAt: "2026-03-28T08:00:00Z",
    approvedAt: "2026-03-29T10:00:00Z",
    paidAt: "2026-03-30T15:00:00Z",
    startedAt: "2026-03-31T09:00:00Z",
  },
  {
    id: 5,
    productId: 6,
    productName: "구찌 GG 마몬트 숄더백",
    productImageUrl: "/images/stub/bag.jpg",
    renterId: 2,
    renterName: "홍길동",
    lenderId: 1,
    lenderName: "김등록",
    status: "RETURNED",
    startDate: "2026-03-10",
    endDate: "2026-03-13",
    dailyPrice: 30000,
    depositAmount: 800000,
    totalAmount: 90000 + 800000,
    deliveryInfo: {
      recipientName: "홍길동",
      recipientPhone: "010-1234-5678",
      address: "서울시 강남구 테헤란로 123",
      addressDetail: "101호",
      zipCode: "06234",
    },
    payment: {
      id: 3,
      paymentKey: "toss-pay-key-ghi789",
      orderId: `RC-5-${Date.now()}`,
      amount: 890000,
      status: "COMPLETED",
      paymentMethod: "카드",
      paidAt: "2026-03-08T12:00:00Z",
    },
    requestedAt: "2026-03-05T09:00:00Z",
    approvedAt: "2026-03-06T11:00:00Z",
    paidAt: "2026-03-08T12:00:00Z",
    startedAt: "2026-03-09T10:00:00Z",
    returnedAt: "2026-03-14T15:00:00Z",
  },
];

// 런타임 중 상태 변경을 위한 가변 복사본
export let stubRentals: Rental[] = [...STUB_RENTALS.map((r) => ({ ...r }))];

export function resetStubRentals(): void {
  stubRentals = [...STUB_RENTALS.map((r) => ({ ...r }))];
  nextRentalId = 100;
}

export function findRentalById(id: number | string): Rental | undefined {
  return stubRentals.find((r) => r.id === Number(id));
}

export function findRentalsByRole(
  role: "renter" | "lender",
  userId: number = 2
): Rental[] {
  if (role === "renter") {
    return stubRentals.filter((r) => r.renterId === userId);
  }
  return stubRentals.filter((r) => r.lenderId === userId);
}

export function updateRentalStatus(
  id: number | string,
  status: RentalStatus,
  extra?: Partial<Rental>
): Rental | undefined {
  const idx = stubRentals.findIndex((r) => r.id === Number(id));
  if (idx < 0) return undefined;
  stubRentals[idx] = { ...stubRentals[idx], status, ...extra };
  return stubRentals[idx];
}
