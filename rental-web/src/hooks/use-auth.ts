"use client";

import { useCallback } from "react";
import { useAuthStore } from "@/stores/auth-store";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type {
  User,
  AuthTokens,
  LoginRequest,
  SignupRequest,
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

  /** 로그인 */
  const login = useCallback(
    async (request: LoginRequest) => {
      setLoading(true);
      setError(null);

      try {
        const tokenResponse = await apiClient.post<AuthTokens>(
          ENDPOINTS.AUTH.LOGIN,
          request,
          { requiresAuth: false }
        );

        const userResponse = await apiClient.get<User>(ENDPOINTS.AUTH.ME);

        storeLogin(userResponse.data, tokenResponse.data);
        return { success: true };
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "로그인에 실패했습니다.";
        setError(message);
        return { success: false, error: message };
      } finally {
        setLoading(false);
      }
    },
    [apiClient, storeLogin, setLoading, setError]
  );

  /** 회원가입 */
  const signup = useCallback(
    async (request: SignupRequest) => {
      setLoading(true);
      setError(null);

      try {
        const tokenResponse = await apiClient.post<AuthTokens>(
          ENDPOINTS.AUTH.SIGNUP,
          request,
          { requiresAuth: false }
        );

        const userResponse = await apiClient.get<User>(ENDPOINTS.AUTH.ME);

        storeLogin(userResponse.data, tokenResponse.data);
        return { success: true };
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "회원가입에 실패했습니다.";
        setError(message);
        return { success: false, error: message };
      } finally {
        setLoading(false);
      }
    },
    [apiClient, storeLogin, setLoading, setError]
  );

  /** 로그아웃 */
  const logout = useCallback(async () => {
    try {
      await apiClient.post(ENDPOINTS.AUTH.LOGOUT);
    } catch {
      // 로그아웃 API 실패해도 로컬 상태는 정리
    }
    storeLogout();
  }, [apiClient, storeLogout]);

  /** 유저 정보 조회 */
  const fetchUser = useCallback(async () => {
    try {
      const response = await apiClient.get<User>(ENDPOINTS.AUTH.ME);
      setUser(response.data);
    } catch {
      // 조회 실패 시 무시
    }
  }, [apiClient, setUser]);

  return {
    user,
    isAuthenticated,
    isLoading,
    error,
    login,
    signup,
    logout,
    fetchUser,
    checkAuth,
  };
}
