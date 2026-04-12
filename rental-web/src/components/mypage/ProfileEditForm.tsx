"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { MyPageInfo, UpdateProfileRequest } from "@/lib/api/types";

// ========================================
// 유효성 검사 스키마
// ========================================

const profileEditSchema = z.object({
  name: z
    .string()
    .min(2, "이름은 2자 이상이어야 합니다.")
    .max(20, "이름은 20자 이하여야 합니다."),
  phone: z
    .string()
    .regex(/^010-\d{4}-\d{4}$/, "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)"),
  profileImageUrl: z.string().url("올바른 URL을 입력하세요.").optional().or(z.literal("")),
});

type ProfileEditFormValues = z.infer<typeof profileEditSchema>;

// ========================================
// ProfileEditForm 컴포넌트
// ========================================

interface ProfileEditFormProps {
  myPageInfo: MyPageInfo;
  onSubmit: (data: UpdateProfileRequest) => Promise<void>;
  isSubmitting?: boolean;
}

export function ProfileEditForm({
  myPageInfo,
  onSubmit,
  isSubmitting = false,
}: ProfileEditFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfileEditFormValues>({
    resolver: zodResolver(profileEditSchema),
    defaultValues: {
      name: myPageInfo.name,
      phone: myPageInfo.phone,
      profileImageUrl: myPageInfo.profileImageUrl ?? "",
    },
  });

  const handleFormSubmit = async (values: ProfileEditFormValues) => {
    await onSubmit({
      name: values.name,
      phone: values.phone,
      profileImageUrl: values.profileImageUrl || undefined,
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>기본 정보 수정</CardTitle>
      </CardHeader>
      <form onSubmit={handleSubmit(handleFormSubmit)}>
        <CardContent className="space-y-4">
          {/* 이름 */}
          <div className="space-y-2">
            <label htmlFor="name" className="text-sm font-medium">
              이름 <span className="text-destructive">*</span>
            </label>
            <Input
              id="name"
              placeholder="이름을 입력하세요"
              {...register("name")}
            />
            {errors.name && (
              <p className="text-xs text-destructive">{errors.name.message}</p>
            )}
          </div>

          {/* 이메일 (읽기 전용) */}
          <div className="space-y-2">
            <label htmlFor="email" className="text-sm font-medium">
              이메일
            </label>
            <Input
              id="email"
              type="email"
              value={myPageInfo.email}
              disabled
              className="bg-muted/50"
            />
            <p className="text-xs text-muted-foreground">이메일은 변경할 수 없습니다.</p>
          </div>

          {/* 전화번호 */}
          <div className="space-y-2">
            <label htmlFor="phone" className="text-sm font-medium">
              전화번호 <span className="text-destructive">*</span>
            </label>
            <Input
              id="phone"
              placeholder="010-0000-0000"
              {...register("phone")}
            />
            {errors.phone && (
              <p className="text-xs text-destructive">{errors.phone.message}</p>
            )}
          </div>

          {/* 프로필 이미지 URL */}
          <div className="space-y-2">
            <label htmlFor="profileImageUrl" className="text-sm font-medium">
              프로필 이미지 URL
            </label>
            <Input
              id="profileImageUrl"
              type="url"
              placeholder="https://example.com/image.jpg"
              {...register("profileImageUrl")}
            />
            {errors.profileImageUrl && (
              <p className="text-xs text-destructive">
                {errors.profileImageUrl.message}
              </p>
            )}
          </div>
        </CardContent>
        <CardFooter>
          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? "저장 중..." : "저장"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
