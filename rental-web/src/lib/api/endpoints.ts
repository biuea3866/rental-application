// ========================================
// API 엔드포인트 상수
// ========================================

const API_VERSION = "/api/v1";

export const ENDPOINTS = {
  // 인증
  AUTH: {
    LOGIN: `${API_VERSION}/auth/login`,
    SIGNUP: `${API_VERSION}/auth/signup`,
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
    BY_ID: (id: string) => `${API_VERSION}/products/${id}`,
    BY_CATEGORY: (category: string) =>
      `${API_VERSION}/products?category=${category}`,
    SEARCH: `${API_VERSION}/products/search`,
    MY_PRODUCTS: `${API_VERSION}/products/mine`,
  },

  // 가이드 가격
  GUIDE_PRICES: {
    BASE: `${API_VERSION}/guide-prices`,
    BY_CATEGORY: (category: string) =>
      `${API_VERSION}/guide-prices/${category}`,
  },

  // 대여
  RENTALS: {
    BASE: `${API_VERSION}/rentals`,
    BY_ID: (id: string) => `${API_VERSION}/rentals/${id}`,
    MY_RENTALS: `${API_VERSION}/rentals/mine`,
  },
} as const;
