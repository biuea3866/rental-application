import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { ChatBubble } from "@/components/chat/ChatBubble";

// ========================================
// ChatBubble 컴포넌트 테스트
// RC-FE-325
// ========================================

describe("ChatBubble", () => {
  const createdAt = "2026-04-14T10:05:00+09:00";

  it("내 메시지는 오른쪽 정렬(justify-end)로 표시", () => {
    const { container } = render(
      <ChatBubble content="안녕하세요" createdAt={createdAt} isMine />
    );
    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper.className).toContain("justify-end");
  });

  it("상대 메시지는 왼쪽 정렬(justify-start)로 표시", () => {
    const { container } = render(
      <ChatBubble content="안녕하세요" createdAt={createdAt} isMine={false} />
    );
    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper.className).toContain("justify-start");
  });

  it("내 메시지 버블은 파란색 배경", () => {
    render(<ChatBubble content="테스트" createdAt={createdAt} isMine />);
    const bubble = screen.getByTestId("chat-bubble-mine");
    const msgDiv = bubble.querySelector(".bg-blue-600");
    expect(msgDiv).not.toBeNull();
  });

  it("상대 메시지 버블은 회색 배경", () => {
    render(
      <ChatBubble content="테스트" createdAt={createdAt} isMine={false} />
    );
    const bubble = screen.getByTestId("chat-bubble-other");
    const msgDiv = bubble.querySelector(".bg-gray-100");
    expect(msgDiv).not.toBeNull();
  });

  it("메시지 내용이 렌더링됨", () => {
    render(<ChatBubble content="안녕하세요!" createdAt={createdAt} isMine />);
    expect(screen.getByText("안녕하세요!")).toBeInTheDocument();
  });
});
