import { describe, it, expect } from "vitest";
import { ENDPOINTS } from "@/lib/api/endpoints";

// ========================================
// Endpoints 상수 테스트
// ========================================

describe("API Endpoints 상수", () => {
  describe("AUTH 엔드포인트", () => {
    it("LOGIN 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.AUTH.LOGIN).toBe("/api/v1/auth/login");
    });

    it("SIGNUP 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.AUTH.SIGNUP).toBe("/api/v1/auth/signup");
    });

    it("VERIFY_PHONE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.AUTH.VERIFY_PHONE).toBe("/api/v1/auth/verify-phone");
    });

    it("SOCIAL_LOGIN 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.AUTH.SOCIAL_LOGIN).toBe("/api/v1/auth/social-login");
    });

    it("REFRESH 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.AUTH.REFRESH).toBe("/api/v1/auth/refresh");
    });

    it("LOGOUT 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.AUTH.LOGOUT).toBe("/api/v1/auth/logout");
    });

    it("ME 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.AUTH.ME).toBe("/api/v1/auth/me");
    });
  });

  describe("PRODUCTS 엔드포인트", () => {
    it("BASE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.PRODUCTS.BASE).toBe("/api/v1/products");
    });

    it("BY_ID 함수가 올바른 URL을 반환해야 한다", () => {
      expect(ENDPOINTS.PRODUCTS.BY_ID("prod-001")).toBe(
        "/api/v1/products/prod-001"
      );
    });

    it("BY_CATEGORY 함수가 올바른 URL을 반환해야 한다", () => {
      expect(ENDPOINTS.PRODUCTS.BY_CATEGORY("ELECTRONICS")).toBe(
        "/api/v1/products?category=ELECTRONICS"
      );
    });

    it("MY_PRODUCTS 엔드포인트가 올바르게 정의되어야 한다 (BE: /api/v1/my-products)", () => {
      expect(ENDPOINTS.PRODUCTS.MY_PRODUCTS).toBe("/api/v1/my-products");
    });
  });

  describe("GUIDE_PRICES 엔드포인트", () => {
    it("BASE 엔드포인트가 올바르게 정의되어야 한다 (BE: /api/v1/price-guides)", () => {
      expect(ENDPOINTS.GUIDE_PRICES.BASE).toBe("/api/v1/price-guides");
    });

    it("BY_CATEGORY 함수가 올바른 URL을 반환해야 한다", () => {
      expect(ENDPOINTS.GUIDE_PRICES.BY_CATEGORY("SPORTS")).toBe(
        "/api/v1/price-guides/SPORTS"
      );
    });
  });

  describe("RENTALS 엔드포인트", () => {
    it("BASE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.RENTALS.BASE).toBe("/api/v1/rentals");
    });

    it("BY_ID 함수가 올바른 URL을 반환해야 한다", () => {
      expect(ENDPOINTS.RENTALS.BY_ID("rental-001")).toBe(
        "/api/v1/rentals/rental-001"
      );
    });

    it("MY_RENTALS 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.RENTALS.MY_RENTALS).toBe("/api/v1/rentals/mine");
    });
  });

  describe("MYPAGE 엔드포인트", () => {
    it("BASE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.MYPAGE.BASE).toBe("/api/v1/mypage");
    });

    it("PROFILE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.MYPAGE.PROFILE).toBe("/api/v1/mypage/profile");
    });

    it("LENDER_PROFILE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.MYPAGE.LENDER_PROFILE).toBe(
        "/api/v1/mypage/lender-profile"
      );
    });

    it("RENTER_PROFILE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.MYPAGE.RENTER_PROFILE).toBe(
        "/api/v1/mypage/renter-profile"
      );
    });
  });

  describe("NOTIFICATIONS 엔드포인트", () => {
    it("BASE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.NOTIFICATIONS.BASE).toBe("/api/v1/notifications");
    });

    it("BY_ID 함수가 올바른 URL을 반환해야 한다", () => {
      expect(ENDPOINTS.NOTIFICATIONS.BY_ID("notif-001")).toBe(
        "/api/v1/notifications/notif-001"
      );
    });

    it("READ 함수가 올바른 URL을 반환해야 한다", () => {
      expect(ENDPOINTS.NOTIFICATIONS.READ("notif-001")).toBe(
        "/api/v1/notifications/notif-001/read"
      );
    });

    it("READ_ALL 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.NOTIFICATIONS.READ_ALL).toBe(
        "/api/v1/notifications/read-all"
      );
    });

    it("UNREAD_COUNT 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.NOTIFICATIONS.UNREAD_COUNT).toBe(
        "/api/v1/notifications/unread-count"
      );
    });
  });

  describe("USERS 엔드포인트", () => {
    it("BASE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.USERS.BASE).toBe("/api/v1/users");
    });

    it("BY_ID 함수가 올바른 URL을 반환해야 한다", () => {
      expect(ENDPOINTS.USERS.BY_ID("user-001")).toBe("/api/v1/users/user-001");
    });

    it("PROFILE 엔드포인트가 올바르게 정의되어야 한다", () => {
      expect(ENDPOINTS.USERS.PROFILE).toBe("/api/v1/users/profile");
    });
  });
});
