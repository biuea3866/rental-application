import { describe, it, expect, vi, beforeEach } from "vitest";

// ========================================
// Chat API 함수 단위 테스트 (TDD)
// RC-FE-323
// ========================================

const sharedMockClient = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
};

vi.mock("@/lib/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/client")>();
  return {
    ...actual,
    getApiClient: () => sharedMockClient,
  };
});

describe("Chat API 함수", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // TC-CHAT-01: createChatRoomApi — 채팅방 생성
  it("TC-CHAT-01: createChatRoomApi — POST /api/v1/chat/rooms 올바른 엔드포인트로 호출", async () => {
    const { createChatRoomApi } = await import("@/lib/api/chat");
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: { chatRoomId: 1, rentalId: 1001, renterId: 11, lenderId: 22, createdAt: "2026-04-14T10:00:00+09:00" },
      timestamp: new Date().toISOString(),
    });

    const request = { rentalId: 1001, renterId: 11, lenderId: 22 };
    const result = await createChatRoomApi(request);

    expect(sharedMockClient.post).toHaveBeenCalledWith(
      "/api/v1/chat/rooms",
      request
    );
    expect(result.data.chatRoomId).toBe(1);
  });

  // TC-CHAT-02: getChatRoomsApi — 채팅방 목록 조회
  it("TC-CHAT-02: getChatRoomsApi — GET /api/v1/chat/rooms 호출", async () => {
    const { getChatRoomsApi } = await import("@/lib/api/chat");
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: [],
      timestamp: new Date().toISOString(),
    });

    await getChatRoomsApi();

    expect(sharedMockClient.get).toHaveBeenCalledWith("/api/v1/chat/rooms");
  });

  // TC-CHAT-03: getChatMessagesApi — 메시지 목록 조회
  it("TC-CHAT-03: getChatMessagesApi — GET /api/v1/chat/rooms/{roomId}/messages 호출", async () => {
    const { getChatMessagesApi } = await import("@/lib/api/chat");
    sharedMockClient.get.mockResolvedValue({
      success: true,
      data: { content: [], totalElements: 0, totalPages: 0 },
      timestamp: new Date().toISOString(),
    });

    await getChatMessagesApi(1, { page: 0, size: 20 });

    expect(sharedMockClient.get).toHaveBeenCalledWith(
      "/api/v1/chat/rooms/1/messages",
      { params: { page: "0", size: "20" } }
    );
  });

  // TC-CHAT-04: sendChatMessageApi — 메시지 전송
  it("TC-CHAT-04: sendChatMessageApi — POST /api/v1/chat/rooms/{roomId}/messages 호출", async () => {
    const { sendChatMessageApi } = await import("@/lib/api/chat");
    sharedMockClient.post.mockResolvedValue({
      success: true,
      data: undefined,
      timestamp: new Date().toISOString(),
    });

    await sendChatMessageApi(1, { content: "안녕하세요" });

    expect(sharedMockClient.post).toHaveBeenCalledWith(
      "/api/v1/chat/rooms/1/messages",
      { content: "안녕하세요" }
    );
  });
});
