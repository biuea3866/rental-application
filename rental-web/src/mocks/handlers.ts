import { getStubClient } from "@/lib/api/client";
import {
  STUB_USERS,
  findUserByEmail,
  generateStubTokens,
  STUB_PASSWORD,
} from "./users";
import {
  STUB_PRODUCTS,
  STUB_GUIDE_PRICES,
  findProductById,
  findProductsByCategory,
  findGuidePriceByCategory,
} from "./products";
import type {
  LoginRequest,
  SignupRequest,
  User,
  ProductCategory,
} from "@/lib/api/types";

// ========================================
// 스텁 핸들러 등록
// ========================================

let currentUser: User | null = null;

/** 테스트 간 상태 격리를 위한 리셋 함수 */
export function resetStubHandlerState(): void {
  currentUser = null;
}

export function registerStubHandlers(): void {
  const client = getStubClient();
  if (!client) return;

  // ---- 인증 ----

  client.registerHandler("/api/v1/auth/login", async (_endpoint, body) => {
    const { email, password } = body as LoginRequest;
    const user = findUserByEmail(email);

    if (!user || password !== STUB_PASSWORD) {
      throw new Error("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    currentUser = user;
    return generateStubTokens();
  });

  client.registerHandler("/api/v1/auth/signup", async (_endpoint, body) => {
    const request = body as SignupRequest;

    if (findUserByEmail(request.email)) {
      throw new Error("이미 등록된 이메일입니다.");
    }

    const newUser: User = {
      id: `user-${Date.now()}`,
      email: request.email,
      name: request.name,
      phone: request.phone,
      role: request.role,
      createdAt: new Date().toISOString(),
    };

    currentUser = newUser;
    return generateStubTokens();
  });

  client.registerHandler("/api/v1/auth/me", async () => {
    if (!currentUser) {
      // 기본 유저 반환
      currentUser = STUB_USERS[0];
    }
    return currentUser;
  });

  client.registerHandler("/api/v1/auth/refresh", async () => {
    return generateStubTokens();
  });

  client.registerHandler("/api/v1/auth/logout", async () => {
    currentUser = null;
    return { success: true };
  });

  // ---- 상품 ----

  client.registerHandler("/api/v1/products", async (endpoint) => {
    // 카테고리 필터링
    const url = new URL(`http://localhost${endpoint}`);
    const category = url.searchParams.get("category");

    if (category) {
      return findProductsByCategory(category as ProductCategory);
    }

    // 개별 상품 조회
    const idMatch = endpoint.match(/\/products\/([^/?]+)/);
    if (idMatch) {
      const product = findProductById(idMatch[1]);
      if (!product) throw new Error("상품을 찾을 수 없습니다.");
      return product;
    }

    // 목록 반환
    return {
      content: STUB_PRODUCTS,
      page: 0,
      size: 20,
      totalElements: STUB_PRODUCTS.length,
      totalPages: 1,
      hasNext: false,
    };
  });

  // ---- 가이드 가격 ----

  client.registerHandler("/api/v1/guide-prices", async (endpoint) => {
    const categoryMatch = endpoint.match(/\/guide-prices\/([^/?]+)/);
    if (categoryMatch) {
      const guidePrice = findGuidePriceByCategory(
        categoryMatch[1] as ProductCategory
      );
      if (!guidePrice)
        throw new Error("해당 카테고리의 가이드 가격을 찾을 수 없습니다.");
      return guidePrice;
    }
    return STUB_GUIDE_PRICES;
  });

  // ---- 유저 ----

  client.registerHandler("/api/v1/users", async (endpoint) => {
    const idMatch = endpoint.match(/\/users\/([^/?]+)/);
    if (idMatch) {
      const user = STUB_USERS.find((u) => u.id === idMatch[1]);
      if (!user) throw new Error("유저를 찾을 수 없습니다.");
      return user;
    }
    return STUB_USERS;
  });
}
