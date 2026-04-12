import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  User,
  AuthTokens,
  LoginRequest,
  SignupRequest,
  RefreshTokenRequest,
} from "./types";

// ========================================
// Auth API 호출
// ========================================

/** 로그인 API */
export async function loginApi(request: LoginRequest) {
  const client = getApiClient();
  return client.post<AuthTokens>(ENDPOINTS.AUTH.LOGIN, request, {
    requiresAuth: false,
  });
}

/** 회원가입 API */
export async function signupApi(request: SignupRequest) {
  const client = getApiClient();
  return client.post<AuthTokens>(ENDPOINTS.AUTH.SIGNUP, request, {
    requiresAuth: false,
  });
}

/** 토큰 갱신 API */
export async function refreshTokenApi(request: RefreshTokenRequest) {
  const client = getApiClient();
  return client.post<AuthTokens>(ENDPOINTS.AUTH.REFRESH, request, {
    requiresAuth: false,
  });
}

/** 로그아웃 API */
export async function logoutApi() {
  const client = getApiClient();
  return client.post<void>(ENDPOINTS.AUTH.LOGOUT);
}

/** 현재 유저 정보 조회 API */
export async function getMeApi() {
  const client = getApiClient();
  return client.get<User>(ENDPOINTS.AUTH.ME);
}
