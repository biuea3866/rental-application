import type { User, AuthTokens } from "@/lib/api/types";

// ========================================
// 유저 스텁 데이터
// ========================================

export const STUB_USERS: User[] = [
  {
    id: "user-lender-001",
    email: "lender@rental.com",
    name: "김대여",
    phone: "010-1234-5678",
    role: "LENDER",
    profileImageUrl: undefined,
    createdAt: "2026-01-15T09:00:00Z",
  },
  {
    id: "user-lender-002",
    email: "lender2@rental.com",
    name: "박렌트",
    phone: "010-2345-6789",
    role: "LENDER",
    profileImageUrl: undefined,
    createdAt: "2026-02-10T14:30:00Z",
  },
  {
    id: "user-renter-001",
    email: "renter@rental.com",
    name: "이빌림",
    phone: "010-3456-7890",
    role: "RENTER",
    profileImageUrl: undefined,
    createdAt: "2026-01-20T11:00:00Z",
  },
  {
    id: "user-renter-002",
    email: "renter2@rental.com",
    name: "최구독",
    phone: "010-4567-8901",
    role: "RENTER",
    profileImageUrl: undefined,
    createdAt: "2026-03-05T08:45:00Z",
  },
];

// ========================================
// 스텁 인증 토큰
// ========================================

export function generateStubTokens(): AuthTokens {
  return {
    accessToken: `stub-access-${Date.now()}`,
    refreshToken: `stub-refresh-${Date.now()}`,
    expiresIn: 3600, // 1시간
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
