"use client";

import type { ChatRoomResponse } from "@/lib/api/types";

// ========================================
// 채팅방 카드 컴포넌트
// ========================================

interface ChatRoomCardProps {
  room: ChatRoomResponse;
  currentUserId: number;
  lastMessage?: string;
  onClick?: () => void;
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    return date.toLocaleTimeString("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
    });
  }
  if (diffDays === 1) return "어제";
  if (diffDays < 7) return `${diffDays}일 전`;
  return date.toLocaleDateString("ko-KR", { month: "short", day: "numeric" });
}

export function ChatRoomCard({
  room,
  currentUserId,
  lastMessage,
  onClick,
}: ChatRoomCardProps) {
  const partnerId =
    room.renterId === currentUserId ? room.lenderId : room.renterId;
  const partnerLabel =
    room.renterId === currentUserId ? "대여자" : "등록자";

  return (
    <button
      data-testid={`chat-room-card-${room.chatRoomId}`}
      onClick={onClick}
      className="w-full flex items-center gap-4 px-4 py-4 bg-white hover:bg-gray-50 active:bg-gray-100 border-b border-gray-100 transition-colors text-left"
    >
      {/* 아바타 */}
      <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
        <span className="text-blue-600 font-semibold text-sm">
          {partnerLabel[0]}
        </span>
      </div>

      {/* 내용 */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between mb-1">
          <span className="text-sm font-semibold text-gray-900 truncate">
            {partnerLabel} #{partnerId}
          </span>
          <span className="text-xs text-gray-400 ml-2 flex-shrink-0">
            {formatTime(room.createdAt)}
          </span>
        </div>
        <p className="text-sm text-gray-500 truncate">
          {lastMessage ?? "채팅을 시작하세요."}
        </p>
      </div>
    </button>
  );
}
