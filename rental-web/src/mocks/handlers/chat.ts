import { http, HttpResponse, delay } from "msw";
import type {
  ChatRoomResponse,
  ChatRoomListResponse,
  ChatMessageResponse,
  ChatMessageListResponse,
  CreateChatRoomRequest,
} from "@/lib/api/types";

// ========================================
// Chat MSW 핸들러
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ========================================
// 스텁 데이터
// ========================================

export const STUB_CHAT_ROOMS: ChatRoomResponse[] = [
  {
    chatRoomId: 1,
    rentalId: 1001,
    renterId: 11,
    lenderId: 22,
    createdAt: "2026-04-14T10:00:00+09:00",
  },
  {
    chatRoomId: 2,
    rentalId: 1002,
    renterId: 11,
    lenderId: 33,
    createdAt: "2026-04-15T09:30:00+09:00",
  },
];

// BE 필드: messageId, sentAt (chatMessageId, createdAt 아님)
export const STUB_CHAT_MESSAGES: ChatMessageResponse[] = [
  {
    messageId: 101,
    chatRoomId: 1,
    senderId: 22,
    content: "안녕하세요! 대여 문의 주셔서 감사합니다.",
    sentAt: "2026-04-14T10:05:00+09:00",
  },
  {
    messageId: 102,
    chatRoomId: 1,
    senderId: 11,
    content: "네, 5월 1일부터 7일간 대여 가능한지 여쭤봤습니다.",
    sentAt: "2026-04-14T10:06:00+09:00",
  },
  {
    messageId: 103,
    chatRoomId: 1,
    senderId: 22,
    content: "해당 기간 대여 가능합니다. 배송 주소를 알려주시면 진행할게요.",
    sentAt: "2026-04-14T10:08:00+09:00",
  },
  {
    messageId: 104,
    chatRoomId: 1,
    senderId: 11,
    content: "감사합니다! 서울시 강남구 테헤란로 123으로 부탁드립니다.",
    sentAt: "2026-04-15T11:00:00+09:00",
  },
];

let stubRooms = [...STUB_CHAT_ROOMS];
let stubMessages = [...STUB_CHAT_MESSAGES];
let nextMessageId = 200;

export function resetChatHandlerState(): void {
  stubRooms = [...STUB_CHAT_ROOMS];
  stubMessages = [...STUB_CHAT_MESSAGES];
  nextMessageId = 200;
}

// ========================================
// MSW 핸들러 — BE 경로: /api/v1/chat-rooms
// ========================================

export const chatHandlers = [
  // 채팅방 생성 — POST /api/v1/chat-rooms
  http.post(`${BASE_URL}/api/v1/chat-rooms`, async ({ request }) => {
    await delay(200);
    const body = (await request.json()) as CreateChatRoomRequest;
    const newRoom: ChatRoomResponse = {
      chatRoomId: stubRooms.length + 10,
      rentalId: body.rentalId,
      renterId: body.renterId,
      lenderId: body.lenderId,
      createdAt: new Date().toISOString(),
    };
    stubRooms.push(newRoom);
    return HttpResponse.json(newRoom, { status: 201 });
  }),

  // 채팅방 목록 조회 — GET /api/v1/chat-rooms (BE: { chatRooms: [...] })
  http.get(`${BASE_URL}/api/v1/chat-rooms`, async () => {
    await delay(150);
    const response: ChatRoomListResponse = { chatRooms: stubRooms };
    return HttpResponse.json(response);
  }),

  // 채팅 메시지 목록 조회 — GET /api/v1/chat-rooms/:roomId/messages
  // BE: GetChatMessagesResult { messages: { content, totalElements, totalPages } }
  http.get(
    `${BASE_URL}/api/v1/chat-rooms/:roomId/messages`,
    async ({ params, request }) => {
      await delay(150);
      const roomId = Number(params.roomId);
      const url = new URL(request.url);
      const page = parseInt(url.searchParams.get("page") ?? "0", 10);
      const size = parseInt(url.searchParams.get("size") ?? "20", 10);

      const roomMessages = stubMessages.filter(
        (m) => m.chatRoomId === roomId
      );
      const totalElements = roomMessages.length;
      const totalPages = Math.ceil(totalElements / size) || 1;
      const start = page * size;
      const content = roomMessages.slice(start, start + size);

      const response: ChatMessageListResponse = {
        messages: {
          content,
          totalElements,
          totalPages,
        },
      };
      return HttpResponse.json(response);
    }
  ),

  // 채팅 메시지 전송 — POST /api/v1/chat-rooms/:roomId/messages
  http.post(
    `${BASE_URL}/api/v1/chat-rooms/:roomId/messages`,
    async ({ params, request }) => {
      await delay(100);
      const roomId = Number(params.roomId);
      const body = (await request.json()) as { content: string };
      const newMessage: ChatMessageResponse = {
        messageId: nextMessageId++,
        chatRoomId: roomId,
        senderId: 11,
        content: body.content,
        sentAt: new Date().toISOString(),
      };
      stubMessages.push(newMessage);
      return HttpResponse.json(newMessage, { status: 201 });
    }
  ),
];
