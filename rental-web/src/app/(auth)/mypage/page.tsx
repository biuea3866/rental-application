"use client";

import { useQuery } from "@tanstack/react-query";
import { MyPageProfile } from "@/components/mypage/MyPageProfile";
import { LenderProfileSection } from "@/components/mypage/LenderProfileSection";
import { RenterProfileSection } from "@/components/mypage/RenterProfileSection";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { MyPageInfo } from "@/lib/api/types";

// ========================================
// 마이페이지
// ========================================

export default function MyPage() {
  const apiClient = getApiClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["mypage"],
    queryFn: async () => {
      const response = await apiClient.get<MyPageInfo>(ENDPOINTS.MYPAGE.BASE);
      return response.data;
    },
  });

  if (isLoading) {
    return (
      <main className="flex flex-1 items-center justify-center p-6">
        <p className="text-sm text-muted-foreground">불러오는 중...</p>
      </main>
    );
  }

  if (isError || !data) {
    return (
      <main className="flex flex-1 items-center justify-center p-6">
        <p className="text-sm text-destructive">
          정보를 불러오지 못했습니다. 다시 시도해주세요.
        </p>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-6 p-6">
      <h1 className="text-2xl font-bold">마이페이지</h1>

      {/* 기본 정보 */}
      <MyPageProfile myPageInfo={data} />

      {/* 역할별 프로필 */}
      {data.role === "LENDER" && (
        <LenderProfileSection lenderProfile={data.lenderProfile} />
      )}
      {data.role === "RENTER" && (
        <RenterProfileSection renterProfile={data.renterProfile} />
      )}
    </main>
  );
}
