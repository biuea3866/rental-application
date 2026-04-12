"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ProfileEditForm } from "@/components/mypage/ProfileEditForm";
import { getApiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import type { MyPageInfo, UpdateProfileRequest } from "@/lib/api/types";

// ========================================
// 프로필 수정 페이지
// ========================================

export default function ProfileEditPage() {
  const router = useRouter();
  const apiClient = getApiClient();
  const queryClient = useQueryClient();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["mypage"],
    queryFn: async () => {
      const response = await apiClient.get<MyPageInfo>(ENDPOINTS.MYPAGE.BASE);
      return response.data;
    },
  });

  const mutation = useMutation({
    mutationFn: async (updateData: UpdateProfileRequest) => {
      const response = await apiClient.patch<MyPageInfo>(
        ENDPOINTS.MYPAGE.PROFILE,
        updateData
      );
      return response.data;
    },
    onSuccess: (updatedData) => {
      queryClient.setQueryData(["mypage"], updatedData);
      toast.success("프로필이 수정되었습니다.");
      router.push("/mypage");
    },
    onError: (err) => {
      const message =
        err instanceof Error ? err.message : "프로필 수정에 실패했습니다.";
      setSubmitError(message);
      toast.error(message);
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
      <h1 className="text-2xl font-bold">프로필 수정</h1>

      {submitError && (
        <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
          {submitError}
        </div>
      )}

      <ProfileEditForm
        myPageInfo={data}
        onSubmit={async (formData) => {
          setSubmitError(null);
          await mutation.mutateAsync(formData);
        }}
        isSubmitting={mutation.isPending}
      />
    </main>
  );
}
