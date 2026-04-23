"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

// ========================================
// Pagination — 페이지네이션 UI (기본 size=20)
// ========================================

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrev: boolean;
  onPageChange: (page: number) => void;
  className?: string;
}

export function Pagination({
  currentPage,
  totalPages,
  hasNext,
  hasPrev,
  onPageChange,
  className,
}: PaginationProps) {
  // 표시할 페이지 번호 범위 계산 (최대 5개)
  const getPageNumbers = (): number[] => {
    const delta = 2;
    const range: number[] = [];
    const start = Math.max(0, currentPage - delta);
    const end = Math.min(totalPages - 1, currentPage + delta);

    for (let i = start; i <= end; i++) {
      range.push(i);
    }
    return range;
  };

  const pageNumbers = getPageNumbers();

  return (
    <nav
      aria-label="페이지 네비게이션"
      className={cn("flex items-center justify-center gap-1", className)}
      data-testid="pagination"
    >
      {/* 이전 페이지 */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={!hasPrev}
        aria-label="이전 페이지"
        data-testid="pagination-prev"
      >
        <ChevronLeft className="size-4" />
      </Button>

      {/* 페이지 번호들 */}
      {pageNumbers.map((page) => (
        <Button
          key={page}
          variant={page === currentPage ? "default" : "outline"}
          size="sm"
          onClick={() => onPageChange(page)}
          aria-label={`${page + 1} 페이지`}
          aria-current={page === currentPage ? "page" : undefined}
          data-testid={`pagination-page-${page}`}
        >
          {page + 1}
        </Button>
      ))}

      {/* 다음 페이지 */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={!hasNext}
        aria-label="다음 페이지"
        data-testid="pagination-next"
      >
        <ChevronRight className="size-4" />
      </Button>
    </nav>
  );
}
