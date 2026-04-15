"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { RenterProfile, ProductCategory } from "@/lib/api/types";

// ========================================
// RenterProfileSection 컴포넌트
// ========================================

const CATEGORY_LABELS: Record<ProductCategory, string> = {
  ELECTRONICS: "전자기기",
  FURNITURE: "가구",
  SPORTS: "스포츠",
  FASHION: "패션",
  BOOKS: "도서",
  TOOLS: "공구",
  VEHICLES: "차량",
  OTHERS: "기타",
};

interface RenterProfileSectionProps {
  renterProfile?: RenterProfile;
  onEdit?: () => void;
}

export function RenterProfileSection({
  renterProfile,
  onEdit,
}: RenterProfileSectionProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>대여자 프로필</CardTitle>
        {onEdit && (
          <Button variant="outline" size="sm" onClick={onEdit}>
            수정
          </Button>
        )}
      </CardHeader>
      <CardContent>
        {renterProfile ? (
          <div className="space-y-3 rounded-lg bg-muted/30 p-4">
            {renterProfile.shippingAddress && (
              <div className="flex justify-between">
                <span className="text-sm font-medium text-muted-foreground">
                  배송 주소
                </span>
                <span className="text-sm">{renterProfile.shippingAddress}</span>
              </div>
            )}
            {renterProfile.preferredCategories &&
              renterProfile.preferredCategories.length > 0 && (
                <div className="flex flex-col gap-2">
                  <span className="text-sm font-medium text-muted-foreground">
                    선호 카테고리
                  </span>
                  <div className="flex flex-wrap gap-1">
                    {renterProfile.preferredCategories.map((cat) => (
                      <span
                        key={cat}
                        className="rounded-full bg-primary/10 px-2 py-0.5 text-xs text-primary"
                      >
                        {CATEGORY_LABELS[cat] ?? cat}
                      </span>
                    ))}
                  </div>
                </div>
              )}
          </div>
        ) : (
          <div className="flex flex-col items-center gap-2 py-6 text-center text-muted-foreground">
            <p className="text-sm">대여자 프로필이 없습니다.</p>
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
