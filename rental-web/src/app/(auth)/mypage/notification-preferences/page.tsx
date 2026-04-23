"use client";

import { useState, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ChannelToggle } from "@/components/mypage/ChannelToggle";
import {
  getNotificationPreferencesApi,
  updateNotificationPreferencesApi,
} from "@/lib/api/notification-preferences";
import type {
  NotificationPreferenceResult,
  UpdateNotificationPreferenceRequest,
} from "@/lib/api/types";

// ========================================
// 알림 설정 페이지 (FE-454, PRD-004 §4.5)
// ========================================

const QUERY_KEY = ["notification-preferences"] as const;

// 채널 정의
const CHANNELS: Array<{
  key: keyof UpdateNotificationPreferenceRequest;
  label: string;
  description?: string;
}> = [
  { key: "chatEnabled", label: "채팅 알림", description: "채팅 메시지 수신 알림" },
  { key: "rentalEnabled", label: "대여 알림", description: "대여 요청·승인·반납 알림" },
  {
    key: "settlementEnabled",
    label: "정산 알림",
    description: "정산 완료 및 입금 알림",
  },
  {
    key: "marketingEnabled",
    label: "마케팅 알림",
    description: "이벤트·프로모션 수신 알림",
  },
];

// ========================================
// Named export — 테스트에서 동적 import 가능
// ========================================

export interface NotificationPreferencesPageProps {
  /** 저장 성공 콜백 (테스트/부모 컴포넌트에서 주입 가능) */
  onSaveSuccess?: () => void;
  /** 저장 실패 콜백 (테스트/부모 컴포넌트에서 주입 가능) */
  onSaveError?: (error: unknown) => void;
}

export function NotificationPreferencesPage({
  onSaveSuccess,
  onSaveError,
}: NotificationPreferencesPageProps = {}) {
  const queryClient = useQueryClient();

  // 낙관적 업데이트 상태 (UI 즉시 반영용)
  const [optimistic, setOptimistic] = useState<
    NotificationPreferenceResult | null
  >(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: QUERY_KEY,
    queryFn: async () => {
      const res = await getNotificationPreferencesApi();
      return res.data;
    },
  });

  const mutation = useMutation({
    mutationFn: (req: UpdateNotificationPreferenceRequest) =>
      updateNotificationPreferencesApi(req),
    onMutate: async (variables) => {
      // 진행 중인 쿼리 취소
      await queryClient.cancelQueries({ queryKey: QUERY_KEY });

      // 이전 값 스냅샷
      const previous =
        queryClient.getQueryData<NotificationPreferenceResult>(QUERY_KEY);

      // 낙관적 업데이트 (로컬 상태 즉시 반영)
      if (previous) {
        const next: NotificationPreferenceResult = { ...previous, ...variables };
        setOptimistic(next);
        queryClient.setQueryData(QUERY_KEY, next);
      }

      return { previous };
    },
    onError: (_err, _variables, context) => {
      // 롤백
      if (context?.previous) {
        queryClient.setQueryData(QUERY_KEY, context.previous);
        setOptimistic(null);
      }
      toast.error("저장에 실패했습니다. 다시 시도해주세요.");
      onSaveError?.(_err);
    },
    onSuccess: () => {
      setOptimistic(null);
      toast.success("저장되었습니다.");
      onSaveSuccess?.();
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });

  const handleToggle = useCallback(
    (key: keyof UpdateNotificationPreferenceRequest, value: boolean) => {
      mutation.mutate({ [key]: value });
    },
    [mutation]
  );

  // 표시할 데이터: 낙관적 값 우선, 없으면 서버 값
  const displayData = optimistic ?? data;

  if (isLoading) {
    return (
      <main className="flex flex-1 items-center justify-center p-6">
        <p className="text-sm text-muted-foreground">불러오는 중...</p>
      </main>
    );
  }

  if (isError || !displayData) {
    return (
      <main className="flex flex-1 items-center justify-center p-6">
        <p className="text-sm text-destructive">
          알림 설정을 불러오지 못했습니다. 다시 시도해주세요.
        </p>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-6 p-6">
      <h1 className="text-2xl font-bold">알림 설정</h1>

      <section className="rounded-lg border bg-card p-4">
        <h2 className="mb-2 text-base font-semibold">알림 채널</h2>

        <div className="divide-y">
          {CHANNELS.map(({ key, label, description }) => (
            <ChannelToggle
              key={key}
              label={label}
              description={description}
              checked={Boolean(
                displayData[key as keyof NotificationPreferenceResult]
              )}
              onChange={(value) => handleToggle(key, value)}
              disabled={mutation.isPending}
            />
          ))}
        </div>
      </section>

      {/* 마케팅 채널 법적 동의 문구 (KISA / 개인정보 수집 동의) */}
      <section
        className="rounded-lg border bg-muted/40 p-4 text-xs text-muted-foreground"
        aria-label="마케팅 동의 안내"
      >
        <p className="mb-1 font-semibold">개인정보 수신 동의 안내</p>
        <p>
          마케팅 알림 수신에 동의하시면 이벤트, 프로모션 등의 광고성 정보를
          이메일·문자·앱 푸시로 수신하실 수 있습니다.
        </p>
        <p className="mt-1">
          개인정보 수집·이용에 대한 동의는 언제든지 철회하실 수 있으며, 동의
          철회 후에도 법령에 따라 일정 기간 보관될 수 있습니다. (정보통신망법
          제50조, KISA 가이드라인 준수)
        </p>
      </section>
    </main>
  );
}

// Next.js 기본 export (페이지 라우팅용)
export default NotificationPreferencesPage;
