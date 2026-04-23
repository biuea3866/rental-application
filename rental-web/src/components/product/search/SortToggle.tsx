"use client";

import { useCallback } from "react";
import { cn } from "@/lib/utils";
import { SORT_OPTIONS } from "@/lib/api/product-search-types";
import type { SortBy, SortDirection } from "@/lib/api/product-search-types";

// ========================================
// SortToggle — 5종 정렬 토글 버튼
// 같은 버튼 재클릭 시 방향(ASC/DESC) 토글
// ========================================

interface SortToggleProps {
  sortBy: SortBy;
  sortDirection: SortDirection;
  onChange: (value: { sortBy: SortBy; sortDirection: SortDirection }) => void;
  className?: string;
}

export function SortToggle({
  sortBy,
  sortDirection,
  onChange,
  className,
}: SortToggleProps) {
  const handleClick = useCallback(
    (option: SortBy) => {
      if (option === sortBy) {
        // 같은 항목 재클릭 → 방향 토글
        onChange({
          sortBy,
          sortDirection: sortDirection === "ASC" ? "DESC" : "ASC",
        });
      } else {
        // 다른 항목 클릭 → 기본 방향 DESC
        onChange({ sortBy: option, sortDirection: "DESC" });
      }
    },
    [sortBy, sortDirection, onChange]
  );

  return (
    <div
      className={cn("space-y-2", className)}
      data-testid="sort-toggle"
    >
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
        정렬
      </p>
      <div
        role="toolbar"
        aria-label="정렬 기준"
        className="flex flex-wrap gap-1.5"
      >
        {SORT_OPTIONS.map((opt) => {
          const isActive = sortBy === opt.sortBy;
          return (
            <button
              key={opt.sortBy}
              type="button"
              aria-pressed={isActive}
              aria-label={`${opt.label} 정렬${isActive ? (sortDirection === "ASC" ? " 오름차순" : " 내림차순") : ""}`}
              onClick={() => handleClick(opt.sortBy)}
              className={cn(
                "inline-flex items-center gap-1 rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                isActive
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-background hover:bg-muted"
              )}
              data-testid={`sort-${opt.sortBy}`}
            >
              {opt.label}
              {isActive && (
                <span aria-hidden="true" className="ml-0.5 text-xs">
                  {sortDirection === "ASC" ? "↑" : "↓"}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
