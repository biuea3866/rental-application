import type { User, AuthTokens } from "@/lib/api/types";

// ========================================
// 유저 스텁 데이터 (BE User 스키마 기준)
// ========================================

export const STUB_USERS: User[] = [
  {
    id: 1,
    email: "lender@rental.com",
    name: "김대여",
    role: "LENDER",
    profileType: "LENDER",
    phone: "010-1234-5678",
    createdAt: "2026-01-15T09:00:00Z",
  },
  {
    id: 2,
    email: "lender2@rental.com",
    name: "박렌트",
    role: "LENDER",
    profileType: "LENDER",
    phone: "010-2345-6789",
    createdAt: "2026-02-10T14:30:00Z",
  },
  {
    id: 3,
    email: "renter@rental.com",
    name: "이빌림",
    role: "RENTER",
    profileType: "RENTER",
    phone: "010-3456-7890",
    createdAt: "2026-01-20T11:00:00Z",
  },
  {
    id: 4,
    email: "renter2@rental.com",
    name: "최구독",
    role: "RENTER",
    profileType: "RENTER",
    phone: "010-4567-8901",
    createdAt: "2026-03-05T08:45:00Z",
  },
];

// ========================================
// 스텁 인증 토큰 (BE AuthTokenResponse 기준)
// ========================================

export function generateStubTokens(userId: number = 1): AuthTokens {
  return {
    accessToken: `stub-access-${Date.now()}`,
    refreshToken: `stub-refresh-${Date.now()}`,
    tokenFamily: `stub-family-${Date.now()}`,
    userId,
  };
}

// ========================================
// 스텁 인증 헬퍼
// ========================================

export function findUserByEmail(email: string): User | undefined {
  return STUB_USERS.find((u) => u.email === email);
}

/** 스텁 비밀번호는 "password123"으로 통일 */
export const STUB_PASSWORD = "password123";
