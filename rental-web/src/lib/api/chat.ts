import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  ChatRoomResponse,
  ChatRoomListResponse,
  ChatMessageListResponse,
  CreateChatRoomRequest,
  SendChatMessageRequest,
} from "./types";

// ========================================
// 채팅 API 호출
// ========================================

/** 1. 채팅방 생성 — POST /api/v1/chat-rooms */
export async function createChatRoomApi(request: CreateChatRoomRequest) {
  const client = getApiClient();
  return client.post<ChatRoomResponse>(ENDPOINTS.CHAT.ROOMS, request);
}

/** 2. 채팅방 목록 조회 — GET /api/v1/chat-rooms (BE: { chatRooms: [...] }) */
export async function getChatRoomsApi() {
  const client = getApiClient();
  return client.get<ChatRoomListResponse>(ENDPOINTS.CHAT.ROOMS);
}

/** 3. 채팅 메시지 목록 조회 — GET /api/v1/chat-rooms/{roomId}/messages?page=&size= */
export async function getChatMessagesApi(
  roomId: string | number,
  params?: { page?: number; size?: number }
) {
  const client = getApiClient();
  const queryParams: Record<string, string> = {};
  if (params?.page !== undefined) queryParams.page = String(params.page);
  if (params?.size !== undefined) queryParams.size = String(params.size);
  return client.get<ChatMessageListResponse>(ENDPOINTS.CHAT.MESSAGES(roomId), {
    params: queryParams,
  });
}

/** 4. 채팅 메시지 전송 — POST /api/v1/chat-rooms/{roomId}/messages */
export async function sendChatMessageApi(
  roomId: string | number,
  request: SendChatMessageRequest
) {
  const client = getApiClient();
  return client.post<void>(ENDPOINTS.CHAT.MESSAGES(roomId), request);
}
