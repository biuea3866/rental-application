"use client";

import { useEffect, useRef, useCallback, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import type { ChatMessageResponse } from "@/lib/api/types";
import { getAccessToken } from "@/lib/auth/token";

// ========================================
// 채팅 WebSocket 훅
// ========================================

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws/chat";

interface UseChatWebSocketOptions {
  roomId: number | string;
  onMessage: (message: ChatMessageResponse) => void;
}

interface UseChatWebSocketReturn {
  isConnected: boolean;
  sendMessage: (content: string) => void;
  disconnect: () => void;
}

export function useChatWebSocket({
  roomId,
  onMessage,
}: UseChatWebSocketOptions): UseChatWebSocketReturn {
  const clientRef = useRef<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const token = getAccessToken();

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: token
        ? { Authorization: `Bearer ${token}` }
        : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        setIsConnected(true);

        // /topic/chat/{roomId} 구독
        client.subscribe(`/topic/chat/${roomId}`, (message: IMessage) => {
          try {
            const parsed = JSON.parse(message.body) as ChatMessageResponse;
            onMessage(parsed);
          } catch {
            // 파싱 실패 무시
          }
        });
      },

      onDisconnect: () => {
        setIsConnected(false);
      },

      onStompError: (frame) => {
        console.error("STOMP 에러:", frame.headers["message"]);
        setIsConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
    // roomId 변경 시 재연결
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  const sendMessage = useCallback(
    (content: string) => {
      if (!clientRef.current?.connected) return;
      clientRef.current.publish({
        destination: "/app/chat.send",
        body: JSON.stringify({ chatRoomId: roomId, content }),
      });
    },
    [roomId]
  );

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
    setIsConnected(false);
  }, []);

  return { isConnected, sendMessage, disconnect };
}
