"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ChatBubble } from "@/components/chat/ChatBubble";
import { ChatInput } from "@/components/chat/ChatInput";
import { DateSeparator } from "@/components/chat/DateSeparator";
import { getChatMessagesApi } from "@/lib/api/chat";
import { useChatWebSocket } from "@/hooks/use-chat-websocket";
import { useAuthStore } from "@/stores/auth-store";
import type { ChatMessageResponse } from "@/lib/api/types";

// ========================================
// 채팅방 상세 페이지
// RC-FE-324 ~ RC-FE-327
// ========================================

function isSameDay(a: string, b: string): boolean {
  return new Date(a).toDateString() === new Date(b).toDateString();
}

export default function ChatRoomPage() {
  const params = useParams();
  const router = useRouter();
  const roomId = Number(params.roomId);
  const user = useAuthStore((s) => s.user);
  const bottomRef = useRef<HTMLDivElement>(null);

  const [liveMessages, setLiveMessages] = useState<ChatMessageResponse[]>([]);

  const { data, isLoading } = useQuery({
    queryKey: ["chat-messages", roomId],
    queryFn: () => getChatMessagesApi(roomId, { page: 0, size: 20 }),
    enabled: !!roomId,
  });

  // BE: GetChatMessagesResult { messages: { content, totalElements, totalPages } }
  const historicalMessages = data?.data?.messages?.content ?? [];

  // WebSocket 메시지 수신
  const handleNewMessage = useCallback((msg: ChatMessageResponse) => {
    setLiveMessages((prev) => {
      // 중복 방지 (BE 필드: messageId)
      if (prev.some((m) => m.messageId === msg.messageId)) return prev;
      return [...prev, msg];
    });
  }, []);

  const { isConnected, sendMessage } = useChatWebSocket({
    roomId,
    onMessage: handleNewMessage,
  });

  // 모든 메시지 병합 (히스토리 + 실시간, 시간순) — BE 필드: messageId, sentAt
  const allMessages = [...historicalMessages, ...liveMessages]
    .filter(
      (msg, idx, arr) =>
        arr.findIndex((m) => m.messageId === msg.messageId) === idx
    )
    .sort(
      (a, b) =>
        new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
    );

  // 새 메시지 수신 시 스크롤 하단으로
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [allMessages.length]);

  const handleSend = (content: string) => {
    sendMessage(content);
  };

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-3 flex items-center gap-3">
          <button
            onClick={() => router.back()}
            className="p-1 -ml-1 text-gray-600 hover:text-gray-900"
            aria-label="뒤로가기"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>
          <div className="flex-1">
            <h1 className="text-base font-semibold text-gray-900">채팅방</h1>
            <p className="text-xs text-gray-400">
              {isConnected ? "연결됨" : "연결 중..."}
            </p>
          </div>
        </div>
      </header>

      {/* 메시지 영역 */}
      <div className="flex-1 overflow-y-auto max-w-lg w-full mx-auto px-4 py-4">
        {isLoading && (
          <div data-testid="loading-state" className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div
                key={i}
                className={`h-10 w-48 rounded-2xl bg-gray-200 animate-pulse ${i % 2 === 0 ? "ml-0" : "ml-auto"}`}
              />
            ))}
          </div>
        )}

        {!isLoading && allMessages.length === 0 && (
          <div
            data-testid="empty-state"
            className="text-center py-12 text-gray-400 text-sm"
          >
            대화를 시작해보세요.
          </div>
        )}

        {!isLoading &&
          allMessages.map((msg, idx) => {
            const prevMsg = allMessages[idx - 1];
            // BE 필드: sentAt
            const showDateSep =
              !prevMsg || !isSameDay(prevMsg.sentAt, msg.sentAt);
            const isMine = msg.senderId === user?.id;

            return (
              <div key={msg.messageId}>
                {showDateSep && <DateSeparator date={msg.sentAt} />}
                <ChatBubble
                  content={msg.content}
                  createdAt={msg.sentAt}
                  isMine={isMine}
                />
              </div>
            );
          })}

        <div ref={bottomRef} />
      </div>

      {/* 입력창 */}
      <div className="max-w-lg w-full mx-auto">
        <ChatInput onSend={handleSend} disabled={!isConnected} />
      </div>
    </div>
  );
}
