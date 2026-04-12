import type { AuthTokens } from "@/lib/api/types";
import { BASE_URL } from "@/lib/api/client";

// ========================================
// JWT 토큰 관리
// ========================================

// Access Token은 메모리에 저장 (보안상 localStorage 사용 안 함)
let accessToken: string | null = null;
let tokenExpiresAt: number | null = null;

// Refresh Token 키 (localStorage 기반 - MVP)
const REFRESH_TOKEN_KEY = "rental_refresh_token";

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

export function setAccessToken(token: string, expiresIn: number): void {
  accessToken = token;
  // expiresIn은 초 단위, 10초 여유를 두고 만료 처리
  tokenExpiresAt = Date.now() + (expiresIn - 10) * 1000;
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
// 토큰 일괄 관리
// ========================================

export function saveTokens(tokens: AuthTokens): void {
  setAccessToken(tokens.accessToken, tokens.expiresIn);
  setRefreshToken(tokens.refreshToken);
}

export function clearTokens(): void {
  accessToken = null;
  tokenExpiresAt = null;
  removeRefreshToken();
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
  if (!refreshToken) {
    clearTokens();
    return null;
  }

  // 중복 갱신 방지 (Token Rotation 대응)
  if (isRefreshing && refreshPromise) {
    return refreshPromise;
  }

  isRefreshing = true;
  refreshPromise = doRefresh(refreshToken);

  try {
    const result = await refreshPromise;
    return result;
  } finally {
    isRefreshing = false;
    refreshPromise = null;
  }
}

async function doRefresh(refreshToken: string): Promise<AuthTokens | null> {
  try {
    const response = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      clearTokens();
      return null;
    }

    const data = await response.json();
    const tokens: AuthTokens = data.data;

    // Token Rotation: 새 토큰 저장
    saveTokens(tokens);
    return tokens;
  } catch {
    clearTokens();
    return null;
  }
}
