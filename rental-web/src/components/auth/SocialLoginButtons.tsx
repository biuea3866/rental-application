"use client";

import { Button } from "@/components/ui/button";
import type { SocialProvider } from "@/lib/api/types";

// ========================================
// 소셜 로그인 버튼 컴포넌트
// ========================================

interface SocialLoginButtonsProps {
  onSocialLogin: (provider: SocialProvider) => void;
  isLoading?: boolean;
}

export function SocialLoginButtons({
  onSocialLogin,
  isLoading = false,
}: SocialLoginButtonsProps) {
  return (
    <div className="space-y-3">
      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t border-border" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-background px-2 text-muted-foreground">
            소셜 로그인
          </span>
        </div>
      </div>

      <Button
        type="button"
        variant="outline"
        className="w-full gap-2 bg-[#FEE500] text-[#3A1D1D] hover:bg-[#FEE500]/90 border-[#FEE500] hover:border-[#FEE500]/90"
        onClick={() => onSocialLogin("KAKAO")}
        disabled={isLoading}
        aria-label="카카오로 로그인"
      >
        <KakaoIcon />
        카카오로 로그인
      </Button>

      <Button
        type="button"
        variant="outline"
        className="w-full gap-2 bg-[#03C75A] text-white hover:bg-[#03C75A]/90 border-[#03C75A] hover:border-[#03C75A]/90"
        onClick={() => onSocialLogin("NAVER")}
        disabled={isLoading}
        aria-label="네이버로 로그인"
      >
        <NaverIcon />
        네이버로 로그인
      </Button>
    </div>
  );
}

// ========================================
// 소셜 아이콘
// ========================================

function KakaoIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
    >
      <path d="M12 3C6.48 3 2 6.69 2 11.25c0 2.93 1.95 5.5 4.9 7.02l-1.25 4.65 5.44-3.6c.62.1 1.26.16 1.91.16 5.52 0 10-3.69 10-8.23S17.52 3 12 3z" />
    </svg>
  );
}

function NaverIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
    >
      <path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727z" />
    </svg>
  );
}
