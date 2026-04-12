"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { LenderProfile } from "@/lib/api/types";

// ========================================
// LenderProfileSection 컴포넌트
// ========================================

interface LenderProfileSectionProps {
  lenderProfile?: LenderProfile;
  onEdit?: () => void;
}

export function LenderProfileSection({
  lenderProfile,
  onEdit,
}: LenderProfileSectionProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>등록자 프로필</CardTitle>
        {onEdit && (
          <Button variant="outline" size="sm" onClick={onEdit}>
            수정
          </Button>
        )}
      </CardHeader>
      <CardContent>
        {lenderProfile ? (
          <div className="space-y-3 rounded-lg bg-muted/30 p-4">
            {lenderProfile.businessName && (
              <div className="flex justify-between">
                <span className="text-sm font-medium text-muted-foreground">
                  상호명
                </span>
                <span className="text-sm">{lenderProfile.businessName}</span>
              </div>
            )}
            {lenderProfile.description && (
              <div className="flex flex-col gap-1">
                <span className="text-sm font-medium text-muted-foreground">
                  소개
                </span>
                <span className="text-sm">{lenderProfile.description}</span>
              </div>
            )}
            {lenderProfile.bankName && (
              <div className="flex justify-between">
                <span className="text-sm font-medium text-muted-foreground">
                  은행
                </span>
                <span className="text-sm">{lenderProfile.bankName}</span>
              </div>
            )}
            {lenderProfile.bankAccount && (
              <div className="flex justify-between">
                <span className="text-sm font-medium text-muted-foreground">
                  계좌번호
                </span>
                <span className="text-sm">{lenderProfile.bankAccount}</span>
              </div>
            )}
          </div>
        ) : (
          <div className="flex flex-col items-center gap-2 py-6 text-center text-muted-foreground">
            <p className="text-sm">등록자 프로필이 없습니다.</p>
            {onEdit && (
              <Button variant="outline" size="sm" onClick={onEdit}>
                프로필 등록
              </Button>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
