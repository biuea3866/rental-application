"use client";

// ========================================
// 채팅 말풍선 컴포넌트
// ========================================

interface ChatBubbleProps {
  content: string;
  createdAt: string;
  isMine: boolean;
}

function formatMessageTime(dateStr: string): string {
  return new Date(dateStr).toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function ChatBubble({ content, createdAt, isMine }: ChatBubbleProps) {
  return (
    <div
      data-testid={`chat-bubble-${isMine ? "mine" : "other"}`}
      className={`flex ${isMine ? "justify-end" : "justify-start"} mb-2`}
    >
      <div
        className={`flex flex-col gap-1 max-w-[75%] ${isMine ? "items-end" : "items-start"}`}
      >
        <div
          className={[
            "px-4 py-2 rounded-2xl text-sm leading-relaxed break-words",
            isMine
              ? "bg-blue-600 text-white rounded-br-sm"
              : "bg-gray-100 text-gray-900 rounded-bl-sm",
          ].join(" ")}
        >
          {content}
        </div>
        <span className="text-xs text-gray-400 px-1">
          {formatMessageTime(createdAt)}
        </span>
      </div>
    </div>
  );
}
