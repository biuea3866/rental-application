"use client";

// ========================================
// StarRating 컴포넌트 — 1~5 별점 인터랙티브 입력
// RC-FE-317
// ========================================

interface StarRatingProps {
  value: number;
  onChange?: (rating: number) => void;
  readonly?: boolean;
  size?: "sm" | "md" | "lg";
}

const SIZE_CLASSES = {
  sm: "w-4 h-4",
  md: "w-6 h-6",
  lg: "w-8 h-8",
};

export function StarRating({
  value,
  onChange,
  readonly = false,
  size = "md",
}: StarRatingProps) {
  const sizeClass = SIZE_CLASSES[size];

  return (
    <div
      data-testid="star-rating"
      className="flex gap-1"
      role={readonly ? undefined : "group"}
      aria-label={readonly ? `별점 ${value}점` : "별점 선택"}
    >
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          data-testid={`star-${star}`}
          disabled={readonly}
          onClick={() => !readonly && onChange?.(star)}
          aria-label={`${star}점`}
          className={[
            sizeClass,
            "transition-colors",
            readonly ? "cursor-default" : "cursor-pointer hover:scale-110",
          ].join(" ")}
        >
          <svg
            viewBox="0 0 24 24"
            fill={star <= value ? "currentColor" : "none"}
            stroke="currentColor"
            strokeWidth={1.5}
            className={[
              "w-full h-full",
              star <= value ? "text-yellow-400" : "text-gray-300",
            ].join(" ")}
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.562.562 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z"
            />
          </svg>
        </button>
      ))}
    </div>
  );
}
