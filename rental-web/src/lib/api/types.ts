// ========================================
// API 공통 타입 정의
// ========================================

/** API 응답 공통 래퍼 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
}

/** 페이지네이션 응답 (Spring Page 구조) */
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  /** Spring Page.last 필드 — 마지막 페이지 여부 */
  last: boolean;
  /** FE 편의용 — last 필드로 계산 */
  hasNext: boolean;
}

/** 에러 응답 */
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, string>;
}

// ========================================
// 인증 관련 타입 (BE AuthTokenResponse 기준)
// ========================================

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  phone: string;
  role: UserRole;
}

/** BE AuthTokenResponse: { accessToken, refreshToken, tokenFamily, userId } */
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenFamily: string;
  userId: number;
}

/** BE RefreshTokenRequest: { refreshToken, tokenFamily } — tokenFamily 필수 */
export interface RefreshTokenRequest {
  refreshToken: string;
  tokenFamily: string;
}

export interface VerifyPhoneRequest {
  phone: string;
  code: string;
}

export type SocialProvider = "KAKAO" | "NAVER";

/**
 * BE SocialLoginRequest: { provider, authorizationCode }
 * FE는 code 필드 사용 유지 (표준 OAuth 필드명), 전송 시 authorizationCode로 매핑
 */
export interface SocialLoginRequest {
  provider: SocialProvider;
  code: string;
}

export interface SendVerificationCodeRequest {
  phone: string;
}

// ========================================
// 유저 관련 타입 (BE /auth/me 응답 기준)
// ========================================

export type UserRole = "LENDER" | "RENTER";

/** BE GET /api/v1/auth/me 응답: { id, email, name, role, profileType } */
export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  /** 프로필 타입 (LENDER/RENTER/NONE 등) */
  profileType?: string;
  phone?: string;
  profileImageUrl?: string;
  createdAt?: string;
}

// ========================================
// 상품 관련 타입 (BE ProductDetailResponse / ProductSummaryResponse 기준)
// ========================================

export type ProductCategory =
  | "ELECTRONICS"
  | "FURNITURE"
  | "SPORTS"
  | "FASHION"
  | "BOOKS"
  | "TOOLS"
  | "VEHICLES"
  | "OTHERS";

/** BE ProductStatus enum */
export type ProductStatus =
  | "DRAFT"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "SUSPENDED";

/** BE RentalUnit enum */
export type RentalUnit = "DAILY" | "WEEKLY" | "MONTHLY";

/** BE ProductCondition enum */
export type ProductCondition = "NEW" | "LIKE_NEW" | "GOOD" | "FAIR" | "POOR";

/** BE ProductDetailResponse.PriceResponse */
export interface PriceResponse {
  id: number;
  rentalUnit: RentalUnit;
  priceAmount: number;
}

/** BE ProductDetailResponse.ImageResponse */
export interface ImageResponse {
  id: number;
  objectKey: string;
  originalFilename: string;
  sortOrder: number;
}

/** BE ProductDetailResponse */
export interface Product {
  id: number;
  userId: number;
  /** 상품 이름 (BE: name) */
  name: string | null;
  description: string | null;
  /** 카테고리 코드 (BE: categoryCode) */
  categoryCode: string | null;
  condition: ProductCondition | null;
  status: ProductStatus;
  /** 보증금 (BE: depositAmount) */
  depositAmount: number | null;
  /** 가격 목록 (BE: prices[]) — DAILY 가격을 대표가로 사용 */
  prices: PriceResponse[];
  /** 이미지 목록 (BE: images[]) */
  images: ImageResponse[];
  createdAt: string;
  updatedAt: string;
}

/** Product 헬퍼: DAILY 가격 조회 */
export function getDailyPrice(product: Product): number | null {
  const daily = product.prices.find((p) => p.rentalUnit === "DAILY");
  return daily ? daily.priceAmount : (product.prices[0]?.priceAmount ?? null);
}

/** Product 헬퍼: 첫 번째 이미지 URL 조회 */
export function getThumbnailUrl(product: Product): string | null {
  const sorted = [...product.images].sort((a, b) => a.sortOrder - b.sortOrder);
  return sorted[0]?.objectKey ?? null;
}

/** BE ProductSummaryResponse (목록 조회) */
export interface ProductSummary {
  id: number;
  name: string | null;
  categoryCode: string | null;
  status: ProductStatus;
  depositAmount: number | null;
  thumbnailUrl: string | null;
  createdAt: string;
}

// ========================================
// 가이드 가격 타입
// ========================================

export interface GuidePriceRange {
  category: ProductCategory;
  minPricePerDay: number;
  maxPricePerDay: number;
  averagePricePerDay: number;
}

/** 상품 등록/수정 요청 — BE ProductRegisterRequest 와 매핑 */
export interface CreateProductRequest {
  name: string;
  description: string;
  categoryCode: string;
  condition: ProductCondition;
  prices: { unit: RentalUnit; amount: number }[];
  depositAmount: number;
  imageKeys: string[];
}

// ========================================
// 마이페이지 타입
// ========================================

export interface LenderProfile {
  businessName?: string;
  description?: string;
  bankName?: string;
  bankAccount?: string;
}

export interface RenterProfile {
  preferredCategories?: ProductCategory[];
  /** BE: shippingAddress */
  shippingAddress?: string;
  trustGrade?: string;
  totalTransactionCount?: number;
}

export interface MyPageInfo {
  id: string;
  email: string;
  name: string;
  phone: string;
  role: UserRole;
  profileImageUrl?: string;
  createdAt: string;
  lenderProfile?: LenderProfile;
  renterProfile?: RenterProfile;
}

export interface UpdateProfileRequest {
  name: string;
  phone: string;
  profileImageUrl?: string;
}

export interface UpdateLenderProfileRequest {
  businessName?: string;
  description?: string;
  bankName?: string;
  bankAccount?: string;
}

export interface UpdateRenterProfileRequest {
  preferredCategories?: ProductCategory[];
  /** BE: shippingAddress */
  shippingAddress?: string;
}

// ========================================
// 대여 관련 타입 (BE Rental/RentalPayment 기준)
// ========================================

export type RentalStatus =
  | "REQUESTED"
  | "APPROVED"
  | "PAID"
  | "IN_USE"
  | "RETURNED"
  | "CANCELLED";

export type PaymentStatus = "PENDING" | "COMPLETED" | "FAILED" | "REFUNDED";

export interface DeliveryInfo {
  recipientName: string;
  recipientPhone: string;
  address: string;
  addressDetail: string;
  zipCode: string;
}

export interface RentalPaymentInfo {
  id: number;
  paymentKey?: string;
  orderId: string;
  amount: number;
  status: PaymentStatus;
  paymentMethod?: string;
  paidAt?: string;
}

export interface Rental {
  id: number;
  productId: number;
  productName: string;
  productImageUrl?: string;
  renterId: number;
  renterName: string;
  lenderId: number;
  lenderName: string;
  status: RentalStatus;
  startDate: string;
  endDate: string;
  dailyPrice: number;
  depositAmount: number;
  totalAmount: number;
  deliveryInfo: DeliveryInfo;
  payment?: RentalPaymentInfo;
  cancelReason?: string;
  requestedAt: string;
  approvedAt?: string;
  paidAt?: string;
  startedAt?: string;
  returnedAt?: string;
  cancelledAt?: string;
}

export interface CreateRentalRequest {
  productId: number;
  startDate: string;
  endDate: string;
  deliveryInfo: DeliveryInfo;
}

export interface ApproveRentalRequest {
  rentalId: number;
}

export interface RejectRentalRequest {
  rentalId: number;
  reason: string;
}

export interface CancelRentalRequest {
  rentalId: number;
  reason?: string;
}

export interface RentalPaymentRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export interface CreateRentalResponse {
  rentalId: number;
  status: RentalStatus;
}

export interface MyRentalsParams {
  role?: "renter" | "lender";
  status?: RentalStatus;
  page?: number;
  size?: number;
}

// ========================================
// 알림 타입
// ========================================

export type NotificationType =
  | "RENTAL_REQUEST"
  | "RENTAL_APPROVED"
  | "RENTAL_REJECTED"
  | "RENTAL_RETURNED"
  | "PAYMENT_COMPLETED"
  | "SYSTEM";

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  relatedId?: string;
}

export interface UnreadCountResponse {
  count: number;
}
