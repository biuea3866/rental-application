"use client";

import { Heart } from "lucide-react";
import { cn } from "@/lib/utils";
import { useWishlistToggle } from "@/hooks/use-wishlist";
import { useAuthStore } from "@/stores/auth-store";
import { useRouter } from "next/navigation";

// ========================================
// WishlistHeartButton
// 상품 카드 / 상세 / 검색결과 공용 컴포넌트
// a11y: aria-pressed, aria-label, 키보드 접근 가능
// ========================================

interface WishlistHeartButtonProps {
  productId: number;
  isWished: boolean;
  className?: string;
  size?: "sm" | "md" | "lg";
}

const SIZE_CLASS = {
  sm: "size-4",
  md: "size-5",
  lg: "size-6",
} as const;

export function WishlistHeartButton({
  productId,
  isWished,
  className,
  size = "md",
}: WishlistHeartButtonProps) {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const { toggle, isPending } = useWishlistToggle({
    productId,
    isAuthenticated,
    onLoginRequired: () => router.push("/login"),
  });

  const handleClick = (e: React.MouseEvent | React.KeyboardEvent) => {
    e.preventDefault();
    e.stopPropagation();
    toggle(isWished);
  };

  return (
    <button
      type="button"
      aria-pressed={isWished}
      aria-label={isWished ? "위시리스트에서 제거" : "위시리스트에 추가"}
      disabled={isPending}
      onClick={handleClick}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          handleClick(e);
        }
      }}
      className={cn(
        "inline-flex items-center justify-center rounded-full p-1.5",
        "transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary",
        "disabled:cursor-not-allowed disabled:opacity-50",
        isWished
          ? "text-red-500 hover:text-red-600"
          : "text-muted-foreground hover:text-red-400",
        className
      )}
    >
      <Heart
        className={cn(SIZE_CLASS[size], isWished && "fill-current")}
        aria-hidden="true"
      />
    </button>
  );
}
