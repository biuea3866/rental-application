"use client";

import { useState, type KeyboardEvent } from "react";

// ========================================
// 채팅 입력 컴포넌트
// ========================================

const MAX_LENGTH = 1000;

interface ChatInputProps {
  onSend: (content: string) => void;
  disabled?: boolean;
}

export function ChatInput({ onSend, disabled }: ChatInputProps) {
  const [value, setValue] = useState("");

  const handleSend = () => {
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setValue("");
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div
      data-testid="chat-input"
      className="flex items-end gap-2 px-4 py-3 bg-white border-t border-gray-200"
    >
      <div className="flex-1 relative">
        <textarea
          data-testid="chat-input-textarea"
          value={value}
          onChange={(e) => {
            if (e.target.value.length <= MAX_LENGTH) {
              setValue(e.target.value);
            }
          }}
          onKeyDown={handleKeyDown}
          placeholder="메시지를 입력하세요..."
          disabled={disabled}
          rows={1}
          className="w-full resize-none rounded-2xl border border-gray-300 px-4 py-2 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed max-h-32 overflow-y-auto"
          style={{ minHeight: "40px" }}
        />
        {value.length > MAX_LENGTH * 0.8 && (
          <span className="absolute bottom-1 right-3 text-xs text-gray-400">
            {value.length}/{MAX_LENGTH}
          </span>
        )}
      </div>
      <button
        data-testid="chat-send-button"
        onClick={handleSend}
        disabled={!value.trim() || disabled}
        className="w-10 h-10 flex-shrink-0 rounded-full bg-blue-600 text-white flex items-center justify-center disabled:opacity-40 disabled:cursor-not-allowed hover:bg-blue-700 active:bg-blue-800 transition-colors"
        aria-label="전송"
      >
        <svg
          className="w-5 h-5"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
          />
        </svg>
      </button>
    </div>
  );
}
