"use client";

import { useCallback } from "react";
import { cn } from "@/lib/utils";
import { CATEGORY_OPTIONS } from "@/lib/api/product-search-types";
import type { ProductCategory } from "@/lib/api/types";

// ========================================
// CategoryFilter — 다중 선택 chip/checkbox
// ========================================

interface CategoryFilterProps {
  selectedCodes: ProductCategory[];
  onChange: (codes: ProductCategory[]) => void;
  className?: string;
}

export function CategoryFilter({
  selectedCodes,
  onChange,
  className,
}: CategoryFilterProps) {
  const toggle = useCallback(
    (code: ProductCategory) => {
      if (selectedCodes.includes(code)) {
        onChange(selectedCodes.filter((c) => c !== code));
      } else {
        onChange([...selectedCodes, code]);
      }
    },
    [selectedCodes, onChange]
  );

  return (
    <fieldset
      className={cn("border-none p-0 m-0", className)}
      role="group"
      aria-label="카테고리 필터"
    >
      <legend className="mb-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
        카테고리
      </legend>
      <div className="flex flex-wrap gap-2">
        {CATEGORY_OPTIONS.map((opt) => {
          const checked = selectedCodes.includes(opt.code);
          return (
            <label
              key={opt.code}
              className={cn(
                "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium cursor-pointer select-none transition-colors",
                checked
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-background hover:bg-muted"
              )}
            >
              <input
                type="checkbox"
                className="sr-only"
                checked={checked}
                aria-label={`${opt.label} 카테고리`}
                onChange={() => toggle(opt.code)}
                data-testid={`category-chip-${opt.code}`}
              />
              {opt.label}
            </label>
          );
        })}
      </div>
    </fieldset>
  );
}
