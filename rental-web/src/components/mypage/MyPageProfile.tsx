"use client";

import Link from "next/link";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { MyPageInfo } from "@/lib/api/types";

// ========================================
// MyPageProfile 컴포넌트
// ========================================

interface MyPageProfileProps {
  myPageInfo: MyPageInfo;
}

export function MyPageProfile({ myPageInfo }: MyPageProfileProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>내 정보</CardTitle>
        <Link
          href="/mypage/edit"
          className="rounded-lg border border-border bg-background px-2.5 py-1 text-[0.8rem] font-medium hover:bg-muted"
        >
          수정
        </Link>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* 프로필 이미지 */}
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted text-2xl font-bold text-muted-foreground">
            {myPageInfo.profileImageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={myPageInfo.profileImageUrl}
                alt={myPageInfo.name}
                className="h-16 w-16 rounded-full object-cover"
              />
            ) : (
              myPageInfo.name.charAt(0)
            )}
          </div>
          <div>
            <p className="text-lg font-semibold">{myPageInfo.name}</p>
            <p className="text-sm text-muted-foreground">
              {myPageInfo.role === "LENDER" ? "등록자" : "대여자"}
            </p>
          </div>
        </div>

        {/* 기본 정보 */}
        <div className="space-y-3 rounded-lg bg-muted/30 p-4">
          <div className="flex justify-between">
            <span className="text-sm font-medium text-muted-foreground">이메일</span>
            <span className="text-sm">{myPageInfo.email}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-sm font-medium text-muted-foreground">전화번호</span>
            <span className="text-sm">{myPageInfo.phone}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-sm font-medium text-muted-foreground">가입일</span>
            <span className="text-sm">
              {new Date(myPageInfo.createdAt).toLocaleDateString("ko-KR")}
            </span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
