"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { cn } from "@/lib/utils";

// ========================================
// PriceRangeInput — 최소/최대 가격 입력
// 300ms debounce 적용
// ========================================

interface PriceRangeInputProps {
  minPrice: number | undefined;
  maxPrice: number | undefined;
  onChange: (min: number | undefined, max: number | undefined) => void;
  className?: string;
}

const DEBOUNCE_MS = 300;

export function PriceRangeInput({
  minPrice,
  maxPrice,
  onChange,
  className,
}: PriceRangeInputProps) {
  const [localMin, setLocalMin] = useState<string>(
    minPrice !== undefined ? String(minPrice) : ""
  );
  const [localMax, setLocalMax] = useState<string>(
    maxPrice !== undefined ? String(maxPrice) : ""
  );

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 외부 props 변경 시 로컬 상태 동기화
  useEffect(() => {
    setLocalMin(minPrice !== undefined ? String(minPrice) : "");
  }, [minPrice]);

  useEffect(() => {
    setLocalMax(maxPrice !== undefined ? String(maxPrice) : "");
  }, [maxPrice]);

  const emitChange = useCallback(
    (minStr: string, maxStr: string) => {
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => {
        const min = minStr !== "" ? Number(minStr) : undefined;
        const max = maxStr !== "" ? Number(maxStr) : undefined;
        onChange(min, max);
      }, DEBOUNCE_MS);
    },
    [onChange]
  );

  const handleMinChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setLocalMin(e.target.value);
    emitChange(e.target.value, localMax);
  };

  const handleMaxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setLocalMax(e.target.value);
    emitChange(localMin, e.target.value);
  };

  const hasError =
    localMin !== "" &&
    localMax !== "" &&
    Number(localMin) > Number(localMax);

  return (
    <div className={cn("space-y-2", className)}>
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
        가격 범위
      </p>
      <div className="flex items-center gap-2">
        <div className="flex-1">
          <label
            htmlFor="price-min"
            className="sr-only"
          >
            최소 가격
          </label>
          <input
            id="price-min"
            type="number"
            aria-label="최소 가격"
            placeholder="최소"
            min={0}
            value={localMin}
            onChange={handleMinChange}
            className={cn(
              "w-full rounded-md border px-3 py-1.5 text-sm",
              "focus:outline-none focus:ring-2 focus:ring-primary",
              hasError ? "border-red-500" : "border-border"
            )}
            data-testid="price-min-input"
          />
        </div>
        <span className="text-muted-foreground text-sm">~</span>
        <div className="flex-1">
          <label
            htmlFor="price-max"
            className="sr-only"
          >
            최대 가격
          </label>
          <input
            id="price-max"
            type="number"
            aria-label="최대 가격"
            placeholder="최대"
            min={0}
            value={localMax}
            onChange={handleMaxChange}
            className={cn(
              "w-full rounded-md border px-3 py-1.5 text-sm",
              "focus:outline-none focus:ring-2 focus:ring-primary",
              hasError ? "border-red-500" : "border-border"
            )}
            data-testid="price-max-input"
          />
        </div>
      </div>
      {hasError && (
        <p
          role="alert"
          className="text-xs text-red-500"
          data-testid="price-range-error"
        >
          최대 가격은 최소 가격보다 커야 합니다.
        </p>
      )}
    </div>
  );
}
