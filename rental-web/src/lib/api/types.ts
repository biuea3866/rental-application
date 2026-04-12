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

/** 페이지네이션 응답 */
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

/** 에러 응답 */
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, string>;
}

// ========================================
// 인증 관련 타입
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

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface VerifyPhoneRequest {
  phone: string;
  code: string;
}

export type SocialProvider = "KAKAO" | "NAVER";

export interface SocialLoginRequest {
  provider: SocialProvider;
  code: string;
  redirectUri: string;
}

export interface SendVerificationCodeRequest {
  phone: string;
}

// ========================================
// 유저 관련 타입
// ========================================

export type UserRole = "LENDER" | "RENTER";

export interface User {
  id: string;
  email: string;
  name: string;
  phone: string;
  role: UserRole;
  profileImageUrl?: string;
  createdAt: string;
}

// ========================================
// 상품 관련 타입
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

export type ProductStatus = "AVAILABLE" | "RENTED" | "UNAVAILABLE";

export interface Product {
  id: string;
  title: string;
  description: string;
  category: ProductCategory;
  pricePerDay: number;
  deposit: number;
  imageUrls: string[];
  status: ProductStatus;
  lenderId: string;
  lenderName: string;
  location: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  title: string;
  description: string;
  category: ProductCategory;
  pricePerDay: number;
  deposit: number;
  imageUrls: string[];
  location: string;
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
