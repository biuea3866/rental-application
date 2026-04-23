"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  fetchWishlist,
  addWishlist,
  removeWishlist,
  isDuplicateWishlistError,
} from "@/lib/api/wishlist";
import type { WishlistPageResult, WishlistResult } from "@/lib/api/types";

// ========================================
// 위시리스트 쿼리 키
// ========================================

export const WISHLIST_QUERY_KEY = ["wishlist"] as const;

export function wishlistListKey(page: number, size: number) {
  return [...WISHLIST_QUERY_KEY, { page, size }] as const;
}

// ========================================
// 위시리스트 목록 훅
// ========================================

export function useWishlistQuery(page = 0, size = 20) {
  return useQuery({
    queryKey: wishlistListKey(page, size),
    queryFn: () => fetchWishlist({ page, size }),
  });
}

// ========================================
// 하트 버튼 토글 훅
// ========================================

interface UseWishlistToggleOptions {
  productId: number;
  isAuthenticated: boolean;
  onLoginRequired?: () => void;
}

export function useWishlistToggle({
  productId,
  isAuthenticated,
  onLoginRequired,
}: UseWishlistToggleOptions) {
  const queryClient = useQueryClient();

  // ── Add 뮤테이션 (낙관적 업데이트)
  const addMutation = useMutation({
    mutationFn: () => addWishlist(productId),

    onMutate: async () => {
      // 진행 중인 fetch 취소
      await queryClient.cancelQueries({ queryKey: WISHLIST_QUERY_KEY });

      // 이전 캐시 스냅샷
      const previousData = queryClient.getQueriesData<WishlistPageResult>({
        queryKey: WISHLIST_QUERY_KEY,
      });

      // 낙관적으로 아이템 추가
      queryClient.setQueriesData<WishlistPageResult>(
        { queryKey: WISHLIST_QUERY_KEY },
        (old) => {
          if (!old) return old;
          const optimisticItem: WishlistResult = {
            id: -1,
            userId: 0,
            productId,
            createdAt: new Date().toISOString(),
          };
          return {
            ...old,
            items: [...old.items, optimisticItem],
            totalElements: old.totalElements + 1,
          };
        }
      );

      return { previousData };
    },

    onError: (error, _variables, context) => {
      // 롤백
      if (context?.previousData) {
        context.previousData.forEach(([queryKey, data]) => {
          queryClient.setQueryData(queryKey, data);
        });
      }

      if (isDuplicateWishlistError(error)) {
        // 409: 이미 추가됨 — 토스트 + 상태 유지 (롤백하지 않음)
        toast.info("이미 위시리스트에 추가된 상품입니다.");
        queryClient.invalidateQueries({ queryKey: WISHLIST_QUERY_KEY });
      } else {
        toast.error("위시리스트 추가에 실패했습니다.");
      }
    },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WISHLIST_QUERY_KEY });
    },
  });

  // ── Remove 뮤테이션 (낙관적 업데이트)
  const removeMutation = useMutation({
    mutationFn: () => removeWishlist(productId),

    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: WISHLIST_QUERY_KEY });

      const previousData = queryClient.getQueriesData<WishlistPageResult>({
        queryKey: WISHLIST_QUERY_KEY,
      });

      // 낙관적으로 아이템 제거
      queryClient.setQueriesData<WishlistPageResult>(
        { queryKey: WISHLIST_QUERY_KEY },
        (old) => {
          if (!old) return old;
          return {
            ...old,
            items: old.items.filter((item) => item.productId !== productId),
            totalElements: Math.max(0, old.totalElements - 1),
          };
        }
      );

      return { previousData };
    },

    onError: (_error, _variables, context) => {
      // 롤백
      if (context?.previousData) {
        context.previousData.forEach(([queryKey, data]) => {
          queryClient.setQueryData(queryKey, data);
        });
      }
      toast.error("위시리스트 삭제에 실패했습니다.");
    },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WISHLIST_QUERY_KEY });
    },
  });

  const toggle = (currentlyWished: boolean) => {
    if (!isAuthenticated) {
      onLoginRequired?.();
      return;
    }

    if (currentlyWished) {
      removeMutation.mutate();
    } else {
      addMutation.mutate();
    }
  };

  const isPending = addMutation.isPending || removeMutation.isPending;

  return { toggle, isPending };
}
