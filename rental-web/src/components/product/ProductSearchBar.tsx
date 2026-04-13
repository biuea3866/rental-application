"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { Search, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

// ========================================
// 자동완성 제안 목록 (하드코딩 샘플)
// ========================================

const SUGGESTIONS = [
  "카메라",
  "맥북",
  "의자",
  "텐트",
  "자전거",
  "가방",
  "드릴",
  "킥보드",
  "프로젝터",
  "토익 교재",
];

// ========================================
// ProductSearchBar 컴포넌트
// ========================================

interface ProductSearchBarProps {
  defaultValue?: string;
  placeholder?: string;
  onSearch: (keyword: string) => void;
  className?: string;
}

export function ProductSearchBar({
  defaultValue = "",
  placeholder = "어떤 물건을 찾으시나요?",
  onSearch,
  className,
}: ProductSearchBarProps) {
  const [value, setValue] = useState(defaultValue);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [filteredSuggestions, setFilteredSuggestions] = useState<string[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // 외부 클릭 시 자동완성 닫기
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // 입력값 변경 시 자동완성 필터링
  const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const v = e.target.value;
    setValue(v);

    if (v.trim().length > 0) {
      const matched = SUGGESTIONS.filter((s) => s.includes(v.trim()));
      setFilteredSuggestions(matched);
      setShowSuggestions(matched.length > 0);
    } else {
      setShowSuggestions(false);
    }
  }, []);

  // 검색 실행
  const handleSearch = useCallback(
    (keyword?: string) => {
      const q = (keyword ?? value).trim();
      setShowSuggestions(false);
      onSearch(q);
    },
    [value, onSearch]
  );

  // Enter 키
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter") {
        handleSearch();
      }
      if (e.key === "Escape") {
        setShowSuggestions(false);
      }
    },
    [handleSearch]
  );

  // 초기화
  const handleClear = useCallback(() => {
    setValue("");
    setShowSuggestions(false);
    onSearch("");
    inputRef.current?.focus();
  }, [onSearch]);

  // 자동완성 선택
  const handleSelectSuggestion = useCallback(
    (s: string) => {
      setValue(s);
      handleSearch(s);
    },
    [handleSearch]
  );

  return (
    <div ref={containerRef} className={cn("relative w-full", className)}>
      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
          <Input
            ref={inputRef}
            value={value}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onFocus={() => {
              if (filteredSuggestions.length > 0 && value.trim())
                setShowSuggestions(true);
            }}
            placeholder={placeholder}
            className="pl-9 pr-8 h-10"
            data-testid="search-input"
            aria-label="상품 검색"
            aria-autocomplete="list"
            aria-expanded={showSuggestions}
          />
          {value && (
            <button
              type="button"
              onClick={handleClear}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              aria-label="검색어 초기화"
              data-testid="clear-button"
            >
              <X className="size-4" />
            </button>
          )}
        </div>
        <Button
          onClick={() => handleSearch()}
          className="h-10 shrink-0"
          data-testid="search-button"
        >
          검색
        </Button>
      </div>

      {/* 자동완성 드롭다운 */}
      {showSuggestions && (
        <ul
          role="listbox"
          className="absolute left-0 right-0 top-full z-50 mt-1 overflow-hidden rounded-lg border bg-popover shadow-md"
          data-testid="autocomplete-list"
        >
          {filteredSuggestions.map((s) => (
            <li
              key={s}
              role="option"
              aria-selected={false}
              onMouseDown={(e) => {
                e.preventDefault();
                handleSelectSuggestion(s);
              }}
              className="flex items-center gap-2 cursor-pointer px-3 py-2 text-sm hover:bg-muted transition-colors"
            >
              <Search className="size-3 text-muted-foreground shrink-0" />
              {s}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
