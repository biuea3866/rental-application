import { http, HttpResponse, delay } from "msw";
import { STUB_USERS } from "../users";
import type {
  MyPageInfo,
  UpdateProfileRequest,
  UpdateLenderProfileRequest,
  UpdateRenterProfileRequest,
} from "@/lib/api/types";

// ========================================
// MyPage MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 스텁 마이페이지 데이터
export const STUB_MYPAGE_INFO: MyPageInfo = {
  ...STUB_USERS[0],
  id: String(STUB_USERS[0].id),
  phone: STUB_USERS[0].phone ?? "",
  createdAt: STUB_USERS[0].createdAt ?? new Date().toISOString(),
  lenderProfile: {
    businessName: "김대여 렌탈샵",
    description: "좋은 물건을 합리적인 가격에 빌려드립니다.",
    bankName: "카카오뱅크",
    bankAccount: "3333-01-1234567",
  },
};

let currentMyPage: MyPageInfo = { ...STUB_MYPAGE_INFO };

export function resetMyPageHandlerState(): void {
  currentMyPage = { ...STUB_MYPAGE_INFO };
}

export const mypageHandlers = [
  // 내 정보 조회
  http.get(`${BASE_URL}/api/v1/mypage`, async () => {
    await delay(150);

    return HttpResponse.json({
      success: true,
      data: currentMyPage,
      timestamp: new Date().toISOString(),
    });
  }),

  // 기본 정보 수정
  http.patch(`${BASE_URL}/api/v1/mypage/profile`, async ({ request }) => {
    await delay(200);

    const body = (await request.json()) as UpdateProfileRequest;

    currentMyPage = {
      ...currentMyPage,
      name: body.name,
      phone: body.phone,
      profileImageUrl: body.profileImageUrl ?? currentMyPage.profileImageUrl,
    };

    return HttpResponse.json({
      success: true,
      data: currentMyPage,
      timestamp: new Date().toISOString(),
    });
  }),

  // 등록자 프로필 수정
  http.patch(`${BASE_URL}/api/v1/mypage/lender-profile`, async ({ request }) => {
    await delay(200);

    const body = (await request.json()) as UpdateLenderProfileRequest;

    currentMyPage = {
      ...currentMyPage,
      lenderProfile: {
        ...currentMyPage.lenderProfile,
        ...body,
      },
    };

    return HttpResponse.json({
      success: true,
      data: currentMyPage,
      timestamp: new Date().toISOString(),
    });
  }),

  // 대여자 프로필 수정
  http.patch(`${BASE_URL}/api/v1/mypage/renter-profile`, async ({ request }) => {
    await delay(200);

    const body = (await request.json()) as UpdateRenterProfileRequest;

    currentMyPage = {
      ...currentMyPage,
      renterProfile: {
        ...currentMyPage.renterProfile,
        ...body,
      },
    };

    return HttpResponse.json({
      success: true,
      data: currentMyPage,
      timestamp: new Date().toISOString(),
    });
  }),
];
