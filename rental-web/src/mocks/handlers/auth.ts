import { http, HttpResponse, delay } from "msw";
import {
  STUB_USERS,
  findUserByEmail,
  generateStubTokens,
  STUB_PASSWORD,
} from "../users";
import type {
  LoginRequest,
  SignupRequest,
  VerifyPhoneRequest,
  SocialLoginRequest,
  User,
} from "@/lib/api/types";

// ========================================
// Auth MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

let currentUser: User | null = null;

/** 테스트 간 상태 격리를 위한 리셋 함수 */
export function resetAuthHandlerState(): void {
  currentUser = null;
}

/** 유효한 인증코드 (테스트용) */
export const STUB_VERIFICATION_CODE = "123456";

export const authHandlers = [
  // 로그인
  http.post(`${BASE_URL}/api/v1/auth/login`, async ({ request }) => {
    await delay(200);

    const body = (await request.json()) as LoginRequest;
    const user = findUserByEmail(body.email);

    if (!user || body.password !== STUB_PASSWORD) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "이메일 또는 비밀번호가 올바르지 않습니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 401 }
      );
    }

    currentUser = user;
    return HttpResponse.json({
      success: true,
      data: generateStubTokens(),
      timestamp: new Date().toISOString(),
    });
  }),

  // 회원가입 (인증코드 발송)
  http.post(`${BASE_URL}/api/v1/auth/signup`, async ({ request }) => {
    await delay(200);

    const body = (await request.json()) as SignupRequest;

    if (findUserByEmail(body.email)) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "이미 등록된 이메일입니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 409 }
      );
    }

    const newUser: User = {
      id: `user-${Date.now()}`,
      email: body.email,
      name: body.name,
      phone: body.phone,
      role: body.role,
      createdAt: new Date().toISOString(),
    };

    currentUser = newUser;
    return HttpResponse.json({
      success: true,
      data: { message: "인증코드가 발송되었습니다." },
      timestamp: new Date().toISOString(),
    });
  }),

  // 휴대폰 인증 완료
  http.post(`${BASE_URL}/api/v1/auth/verify-phone`, async ({ request }) => {
    await delay(200);

    const body = (await request.json()) as VerifyPhoneRequest;

    if (body.code !== STUB_VERIFICATION_CODE) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "인증코드가 올바르지 않습니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      success: true,
      data: generateStubTokens(),
      timestamp: new Date().toISOString(),
    });
  }),

  // 소셜 로그인
  http.post(`${BASE_URL}/api/v1/auth/social-login`, async ({ request }) => {
    await delay(300);

    const body = (await request.json()) as SocialLoginRequest;

    if (!body.code || !body.provider) {
      return HttpResponse.json(
        {
          success: false,
          data: null,
          message: "소셜 로그인 정보가 올바르지 않습니다.",
          timestamp: new Date().toISOString(),
        },
        { status: 400 }
      );
    }

    // 소셜 로그인 성공 시 첫 번째 스텁 유저로 처리
    currentUser = STUB_USERS[0];
    return HttpResponse.json({
      success: true,
      data: generateStubTokens(),
      timestamp: new Date().toISOString(),
    });
  }),

  // 현재 유저 조회
  http.get(`${BASE_URL}/api/v1/auth/me`, async () => {
    await delay(100);

    if (!currentUser) {
      currentUser = STUB_USERS[0];
    }

    return HttpResponse.json({
      success: true,
      data: currentUser,
      timestamp: new Date().toISOString(),
    });
  }),

  // 토큰 갱신
  http.post(`${BASE_URL}/api/v1/auth/refresh`, async () => {
    await delay(100);

    return HttpResponse.json({
      success: true,
      data: generateStubTokens(),
      timestamp: new Date().toISOString(),
    });
  }),

  // 로그아웃
  http.post(`${BASE_URL}/api/v1/auth/logout`, async () => {
    await delay(100);

    currentUser = null;
    return HttpResponse.json({
      success: true,
      data: null,
      timestamp: new Date().toISOString(),
    });
  }),
];
