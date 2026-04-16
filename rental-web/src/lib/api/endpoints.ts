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
    MY_RENTALS: `${API_VERSION}/rentals/mine`,
    APPROVE: (id: string | number) => `${API_VERSION}/rentals/${id}/approve`,
    REJECT: (id: string | number) => `${API_VERSION}/rentals/${id}/reject`,
    CANCEL: (id: string | number) => `${API_VERSION}/rentals/${id}/cancel`,
    PAYMENT: (id: string | number) => `${API_VERSION}/rentals/${id}/payment`,
    START: (id: string | number) => `${API_VERSION}/rentals/${id}/start`,
    RETURN: (id: string | number) => `${API_VERSION}/rentals/${id}/return`,
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
