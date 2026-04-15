"use client";

import { useCallback } from "react";
import { useAuthStore } from "@/stores/auth-store";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { saveTokens } from "@/lib/auth/token";
import type {
  User,
  AuthTokens,
  LoginRequest,
  SignupRequest,
  SocialLoginRequest,
} from "@/lib/api/types";

// ========================================
// useAuth 커스텀 훅
// ========================================

export function useAuth() {
  const {
    user,
    isAuthenticated,
    isLoading,
    error,
    login: storeLogin,
    logout: storeLogout,
    setLoading,
    setError,
    setUser,
    checkAuth,
  } = useAuthStore();

  const apiClient = getApiClient();

  /**
   * 로그인 후 /auth/me 로 유저 정보 조회
   * BE GET /api/v1/auth/me 응답: { id, email, name, role, profileType }
   */
  const fetchMe = useCallback(async (): Promise<User | null> => {
    try {
      const response = await apiClient.get<User>(ENDPOINTS.AUTH.ME);
      return response.data;
    } catch {
      // BE 미구현이거나 오류 시 null 반환
      return null;
    }
  }, [apiClient]);

  /** 로그인 */
  const login = useCallback(
    async (request: LoginRequest) => {
      setLoading(true);
      setError(null);

      try {
        // 1. 토큰 발급 — BE AuthTokenResponse: { accessToken, refreshToken, tokenFamily, userId }
        const tokenResponse = await apiClient.post<AuthTokens>(
          ENDPOINTS.AUTH.LOGIN,
          request,
          { requiresAuth: false }
        );

        // 2. 토큰 먼저 저장 (tokenFamily 포함)
        saveTokens(tokenResponse.data);

        // 3. 유저 정보 조회 — BE GET /api/v1/auth/me
        const userInfo = await fetchMe();

        // 4. 스토어에 유저 + 토큰 저장
        //    /auth/me 가 아직 BE 미구현이면 userId 기반 최소 User 객체 사용
        const user: User = userInfo ?? {
          id: tokenResponse.data.userId,
          email: request.email,
          name: "",
          role: "RENTER",
        };

        storeLogin(user, tokenResponse.data);
        return { success: true };
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "로그인에 실패했습니다.";
        setError(message);
        setLoading(false);
        return { success: false, error: message };
      }
    },
    [apiClient, storeLogin, setLoading, setError, fetchMe]
  );

  /** 회원가입 */
  const signup = useCallback(
    async (request: SignupRequest) => {
      setLoading(true);
      setError(null);

      try {
        // 1. 토큰 발급
        const tokenResponse = await apiClient.post<AuthTokens>(
          ENDPOINTS.AUTH.SIGNUP,
          request,
          { requiresAuth: false }
        );

        // 2. 토큰 저장 (tokenFamily 포함)
        saveTokens(tokenResponse.data);

        // 3. 유저 정보 조회
        const userInfo = await fetchMe();

        const user: User = userInfo ?? {
          id: tokenResponse.data.userId,
          email: request.email,
          name: request.name,
          role: request.role,
        };

        // 4. 스토어에 저장
        storeLogin(user, tokenResponse.data);
        return { success: true };
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "회원가입에 실패했습니다.";
        setError(message);
        setLoading(false);
        return { success: false, error: message };
      }
    },
    [apiClient, storeLogin, setLoading, setError, fetchMe]
  );

  /**
   * 소셜 로그인
   * FE: SocialLoginRequest.code → BE: authorizationCode 로 변환하여 전송
   */
  const socialLogin = useCallback(
    async (request: SocialLoginRequest) => {
      setLoading(true);
      setError(null);

      try {
        // BE SocialLoginRequest: { provider, authorizationCode }
        const beRequest = {
          provider: request.provider,
          authorizationCode: request.code,
        };

        const tokenResponse = await apiClient.post<AuthTokens>(
          ENDPOINTS.AUTH.SOCIAL_LOGIN,
          beRequest,
          { requiresAuth: false }
        );

        saveTokens(tokenResponse.data);

        const userInfo = await fetchMe();

        const user: User = userInfo ?? {
          id: tokenResponse.data.userId,
          email: "",
          name: "",
          role: "RENTER",
        };

        storeLogin(user, tokenResponse.data);
        return { success: true };
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "소셜 로그인에 실패했습니다.";
        setError(message);
        setLoading(false);
        return { success: false, error: message };
      }
    },
    [apiClient, storeLogin, setLoading, setError, fetchMe]
  );

  /** 로그아웃 */
  const logout = useCallback(async () => {
    try {
      await apiClient.post(ENDPOINTS.AUTH.LOGOUT);
    } catch {
      // 로그아웃 API 실패(미구현 포함)해도 로컬 상태는 정리
    }
    storeLogout();
  }, [apiClient, storeLogout]);

  /** 유저 정보 조회 */
  const fetchUser = useCallback(async () => {
    const userInfo = await fetchMe();
    if (userInfo) {
      setUser(userInfo);
    }
  }, [fetchMe, setUser]);

  return {
    user,
    isAuthenticated,
    isLoading,
    error,
    login,
    signup,
    socialLogin,
    logout,
    fetchUser,
    checkAuth,
  };
}
