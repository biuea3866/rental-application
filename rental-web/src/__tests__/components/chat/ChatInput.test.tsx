import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ChatInput } from "@/components/chat/ChatInput";

// ========================================
// ChatInput 컴포넌트 테스트
// RC-FE-326
// ========================================

describe("ChatInput", () => {
  it("초기 상태: 전송 버튼 비활성화", () => {
    render(<ChatInput onSend={vi.fn()} />);
    expect(screen.getByTestId("chat-send-button")).toBeDisabled();
  });

  it("텍스트 입력 후 전송 버튼 활성화", () => {
    render(<ChatInput onSend={vi.fn()} />);
    const textarea = screen.getByTestId("chat-input-textarea");
    fireEvent.change(textarea, { target: { value: "안녕하세요" } });
    expect(screen.getByTestId("chat-send-button")).not.toBeDisabled();
  });

  it("전송 버튼 클릭 시 onSend 콜백 호출", () => {
    const onSend = vi.fn();
    render(<ChatInput onSend={onSend} />);
    const textarea = screen.getByTestId("chat-input-textarea");
    fireEvent.change(textarea, { target: { value: "테스트 메시지" } });
    fireEvent.click(screen.getByTestId("chat-send-button"));
    expect(onSend).toHaveBeenCalledWith("테스트 메시지");
  });

  it("전송 후 입력창 초기화", () => {
    render(<ChatInput onSend={vi.fn()} />);
    const textarea = screen.getByTestId("chat-input-textarea") as HTMLTextAreaElement;
    fireEvent.change(textarea, { target: { value: "메시지" } });
    fireEvent.click(screen.getByTestId("chat-send-button"));
    expect(textarea.value).toBe("");
  });

  it("disabled 상태에서는 입력 불가", () => {
    render(<ChatInput onSend={vi.fn()} disabled />);
    expect(screen.getByTestId("chat-input-textarea")).toBeDisabled();
  });

  it("공백만 입력 시 onSend 미호출", () => {
    const onSend = vi.fn();
    render(<ChatInput onSend={onSend} />);
    const textarea = screen.getByTestId("chat-input-textarea");
    fireEvent.change(textarea, { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("chat-send-button"));
    expect(onSend).not.toHaveBeenCalled();
  });
});
