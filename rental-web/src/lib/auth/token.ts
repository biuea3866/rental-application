import type { AuthTokens } from "@/lib/api/types";
import { BASE_URL } from "@/lib/api/client";

// ========================================
// JWT 토큰 관리
// ========================================

// Access Token은 메모리에 저장 (보안상 localStorage 사용 안 함)
let accessToken: string | null = null;
// BE는 expiresIn을 반환하지 않으므로 30분 고정 TTL 사용 (MVP)
const ACCESS_TOKEN_TTL_MS = 30 * 60 * 1000;
let tokenExpiresAt: number | null = null;

// localStorage 키
const REFRESH_TOKEN_KEY = "rental_refresh_token";
const TOKEN_FAMILY_KEY = "rental_token_family";
const USER_ID_KEY = "rental_user_id";

// ========================================
// Access Token 관리
// ========================================

export function getAccessToken(): string | null {
  if (accessToken && tokenExpiresAt && Date.now() < tokenExpiresAt) {
    return accessToken;
  }
  // 만료된 경우 null 반환
  if (tokenExpiresAt && Date.now() >= tokenExpiresAt) {
    accessToken = null;
    tokenExpiresAt = null;
  }
  return accessToken;
}

export function setAccessToken(token: string, ttlMs: number = ACCESS_TOKEN_TTL_MS): void {
  accessToken = token;
  // ttlMs(밀리초), 10초 여유를 두고 만료 처리
  tokenExpiresAt = Date.now() + ttlMs - 10_000;
}

export function isAccessTokenExpired(): boolean {
  if (!tokenExpiresAt) return true;
  return Date.now() >= tokenExpiresAt;
}

// ========================================
// Refresh Token 관리 (MVP: localStorage)
// ========================================

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
}

export function removeRefreshToken(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

// ========================================
// tokenFamily 관리 (localStorage) — BE refresh 필수 필드
// ========================================

export function getTokenFamily(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_FAMILY_KEY);
}

export function setTokenFamily(family: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_FAMILY_KEY, family);
}

export function removeTokenFamily(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_FAMILY_KEY);
}

// ========================================
// userId 관리 (localStorage)
// ========================================

export function getUserId(): number | null {
  if (typeof window === "undefined") return null;
  const val = localStorage.getItem(USER_ID_KEY);
  return val ? Number(val) : null;
}

export function setUserId(id: number): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(USER_ID_KEY, String(id));
}

export function removeUserId(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(USER_ID_KEY);
}

// ========================================
// 토큰 일괄 관리
// ========================================

export function saveTokens(tokens: AuthTokens): void {
  setAccessToken(tokens.accessToken);
  setRefreshToken(tokens.refreshToken);
  setTokenFamily(tokens.tokenFamily);
  setUserId(tokens.userId);
}

export function clearTokens(): void {
  accessToken = null;
  tokenExpiresAt = null;
  removeRefreshToken();
  removeTokenFamily();
  removeUserId();
}

export function hasValidTokens(): boolean {
  return getAccessToken() !== null || getRefreshToken() !== null;
}

// ========================================
// 토큰 자동 갱신
// ========================================

let isRefreshing = false;
let refreshPromise: Promise<AuthTokens | null> | null = null;

export async function refreshAccessToken(): Promise<AuthTokens | null> {
  const refreshToken = getRefreshToken();
  const tokenFamily = getTokenFamily();

  if (!refreshToken || !tokenFamily) {
    clearTokens();
    return null;
  }

  // 중복 갱신 방지 (Token Rotation 대응)
  if (isRefreshing && refreshPromise) {
    return refreshPromise;
  }

  isRefreshing = true;
  refreshPromise = doRefresh(refreshToken, tokenFamily);

  try {
    const result = await refreshPromise;
    return result;
  } finally {
    isRefreshing = false;
    refreshPromise = null;
  }
}

async function doRefresh(refreshToken: string, tokenFamily: string): Promise<AuthTokens | null> {
  try {
    const response = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken, tokenFamily }),
    });

    if (!response.ok) {
      clearTokens();
      return null;
    }

    const data = await response.json();
    const tokens: AuthTokens = data.data ?? data;

    // Token Rotation: 새 토큰 저장
    saveTokens(tokens);
    return tokens;
  } catch {
    clearTokens();
    return null;
  }
}
