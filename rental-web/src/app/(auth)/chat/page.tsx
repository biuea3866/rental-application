"use client";

import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { ChatRoomCard } from "@/components/chat/ChatRoomCard";
import { getChatRoomsApi } from "@/lib/api/chat";
import { useAuthStore } from "@/stores/auth-store";

// ========================================
// 채팅방 목록 페이지
// RC-FE-323
// ========================================

export default function ChatListPage() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["chat-rooms"],
    queryFn: getChatRoomsApi,
  });

  // BE: GetChatRoomsResult { chatRooms: [...] }
  const rooms = data?.data?.chatRooms ?? [];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-lg mx-auto px-4 py-4">
          <h1 className="text-lg font-semibold text-gray-900">채팅</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto">
        {/* 로딩 */}
        {isLoading && (
          <div data-testid="loading-state" className="space-y-px">
            {[...Array(5)].map((_, i) => (
              <div
                key={i}
                className="h-20 bg-white border-b border-gray-100 animate-pulse"
              />
            ))}
          </div>
        )}

        {/* 에러 */}
        {isError && !isLoading && (
          <div
            data-testid="error-state"
            className="text-center py-16 px-4"
          >
            <p className="text-gray-500 mb-4">채팅 목록을 불러오지 못했습니다.</p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700"
            >
              다시 시도
            </button>
          </div>
        )}

        {/* 빈 상태 */}
        {!isLoading && !isError && rooms.length === 0 && (
          <div
            data-testid="empty-state"
            className="text-center py-16 px-4"
          >
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg
                className="w-8 h-8 text-gray-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                />
              </svg>
            </div>
            <p className="text-gray-500 text-sm">진행 중인 채팅이 없습니다.</p>
          </div>
        )}

        {/* 채팅방 목록 */}
        {!isLoading && !isError && rooms.length > 0 && (
          <div data-testid="chat-room-list" className="bg-white">
            {rooms.map((room) => (
              <ChatRoomCard
                key={room.chatRoomId}
                room={room}
                currentUserId={user?.id ?? 0}
                onClick={() => router.push(`/chat/${room.chatRoomId}`)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
