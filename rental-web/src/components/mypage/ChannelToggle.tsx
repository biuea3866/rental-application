"use client";

// ========================================
// ChannelToggle — 알림 채널 토글 컴포넌트
// a11y: role=switch + aria-checked + aria-label
// ========================================

export interface ChannelToggleProps {
  /** 채널 레이블 (aria-label에도 사용) */
  label: string;
  /** 활성 여부 */
  checked: boolean;
  /** 토글 변경 시 콜백 */
  onChange: (value: boolean) => void;
  /** 비활성화 여부 (저장 중 등) */
  disabled?: boolean;
  /** 설명 텍스트 */
  description?: string;
}

export function ChannelToggle({
  label,
  checked,
  onChange,
  disabled = false,
  description,
}: ChannelToggleProps) {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLButtonElement>) => {
    if (e.key === " " || e.key === "Enter") {
      e.preventDefault();
      if (!disabled) {
        onChange(!checked);
      }
    }
  };

  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div className="flex flex-col gap-0.5">
        <span className="text-sm font-medium text-foreground">{label}</span>
        {description && (
          <span className="text-xs text-muted-foreground">{description}</span>
        )}
      </div>

      <button
        role="switch"
        aria-checked={checked}
        aria-label={label}
        disabled={disabled}
        onClick={() => !disabled && onChange(!checked)}
        onKeyDown={handleKeyDown}
        className={[
          "relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full",
          "transition-colors duration-200 ease-in-out",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
          "disabled:cursor-not-allowed disabled:opacity-50",
          checked ? "bg-primary" : "bg-input",
        ].join(" ")}
      >
        <span
          className={[
            "pointer-events-none inline-block h-5 w-5 transform rounded-full",
            "bg-background shadow-lg ring-0 transition duration-200 ease-in-out",
            checked ? "translate-x-5" : "translate-x-0.5",
          ].join(" ")}
          aria-hidden="true"
        />
      </button>
    </div>
  );
}
