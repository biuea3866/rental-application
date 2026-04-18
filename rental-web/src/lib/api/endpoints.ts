// ========================================
// API 엔드포인트 상수
// ========================================

const API_VERSION = "/api/v1";

export const ENDPOINTS = {
  // 인증
  AUTH: {
    LOGIN: `${API_VERSION}/auth/login`,
    SIGNUP: `${API_VERSION}/auth/signup`,
    VERIFY_PHONE: `${API_VERSION}/auth/verify-phone`,
    SOCIAL_LOGIN: `${API_VERSION}/auth/social-login`,
    REFRESH: `${API_VERSION}/auth/refresh`,
    LOGOUT: `${API_VERSION}/auth/logout`,
    ME: `${API_VERSION}/auth/me`,
  },

  // 유저
  USERS: {
    BASE: `${API_VERSION}/users`,
    BY_ID: (id: string) => `${API_VERSION}/users/${id}`,
    PROFILE: `${API_VERSION}/users/profile`,
  },

  // 상품
  PRODUCTS: {
    BASE: `${API_VERSION}/products`,
    BY_ID: (id: string | number) => `${API_VERSION}/products/${id}`,
    BY_CATEGORY: (category: string) =>
      `${API_VERSION}/products?category=${category}`,
    /** BE: GET /api/v1/my-products */
    MY_PRODUCTS: `${API_VERSION}/my-products`,
  },

  // 가이드 가격 (BE: /api/v1/price-guides)
  GUIDE_PRICES: {
    BASE: `${API_VERSION}/price-guides`,
    BY_CATEGORY: (category: string) =>
      `${API_VERSION}/price-guides/${category}`,
  },

  // 대여
  RENTALS: {
    BASE: `${API_VERSION}/rentals`,
    BY_ID: (id: string | number) => `${API_VERSION}/rentals/${id}`,
    /** BUG-S2-002: BE는 GET /api/v1/rentals?role=RENTER|LENDER 파라미터를 지원하도록 수정 중.
     *  FE는 BASE(/api/v1/rentals)에 role 쿼리 파라미터를 추가하여 호출 — 일치 확인됨. */
    MY_RENTALS: `${API_VERSION}/rentals`,
    APPROVE: (id: string | number) => `${API_VERSION}/rentals/${id}/approve`,
    REJECT: (id: string | number) => `${API_VERSION}/rentals/${id}/reject`,
    PAYMENT: (id: string | number) => `${API_VERSION}/rentals/${id}/payment`,
    START: (id: string | number) => `${API_VERSION}/rentals/${id}/start`,
    RETURN: (id: string | number) => `${API_VERSION}/rentals/${id}/return`,
    /** BUG-S2-001: BE가 PATCH /cancel 엔드포인트를 추가하도록 수정 중. FE는 이미 올바른 메서드 사용. */
    CANCEL: (id: string | number) => `${API_VERSION}/rentals/${id}/cancel`,
  },

  // 마이페이지
  MYPAGE: {
    BASE: `${API_VERSION}/mypage`,
    PROFILE: `${API_VERSION}/mypage/profile`,
    LENDER_PROFILE: `${API_VERSION}/mypage/lender-profile`,
    RENTER_PROFILE: `${API_VERSION}/mypage/renter-profile`,
  },

  // 알림
  NOTIFICATIONS: {
    BASE: `${API_VERSION}/notifications`,
    BY_ID: (id: string) => `${API_VERSION}/notifications/${id}`,
    READ: (id: string) => `${API_VERSION}/notifications/${id}/read`,
    READ_ALL: `${API_VERSION}/notifications/read-all`,
    UNREAD_COUNT: `${API_VERSION}/notifications/unread-count`,
  },
} as const;
