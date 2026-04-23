"use client";

import { cn } from "@/lib/utils";
import { REGION_LABELS } from "@/lib/api/product-search-types";
import type { RegionCode } from "@/lib/api/product-search-types";

// ========================================
// RegionDropdown — 지역 선택 드롭다운
// ========================================

interface RegionDropdownProps {
  value: RegionCode | undefined;
  onChange: (region: RegionCode | undefined) => void;
  className?: string;
}

export function RegionDropdown({
  value,
  onChange,
  className,
}: RegionDropdownProps) {
  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const v = e.target.value;
    onChange(v === "" ? undefined : (v as RegionCode));
  };

  return (
    <div className={cn("space-y-2", className)}>
      <label
        htmlFor="region-select"
        className="block text-xs font-semibold text-muted-foreground uppercase tracking-wider"
      >
        지역
      </label>
      <select
        id="region-select"
        aria-label="지역 선택"
        value={value ?? ""}
        onChange={handleChange}
        className={cn(
          "w-full rounded-md border border-border px-3 py-1.5 text-sm bg-background",
          "focus:outline-none focus:ring-2 focus:ring-primary"
        )}
        data-testid="region-select"
      >
        <option value="">전체 지역</option>
        {(Object.entries(REGION_LABELS) as [RegionCode, string][]).map(
          ([code, label]) => (
            <option key={code} value={code}>
              {label}
            </option>
          )
        )}
      </select>
    </div>
  );
}
