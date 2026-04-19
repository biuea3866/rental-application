"use client";

// ========================================
// 날짜 구분선 컴포넌트
// ========================================

interface DateSeparatorProps {
  date: string;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
  });
}

export function DateSeparator({ date }: DateSeparatorProps) {
  return (
    <div
      data-testid="date-separator"
      className="flex items-center gap-3 my-4"
    >
      <div className="flex-1 h-px bg-gray-200" />
      <span className="text-xs text-gray-400 font-medium px-2 whitespace-nowrap">
        {formatDate(date)}
      </span>
      <div className="flex-1 h-px bg-gray-200" />
    </div>
  );
}
