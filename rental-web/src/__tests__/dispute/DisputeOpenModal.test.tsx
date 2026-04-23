import { describe, it, expect, vi } from "vitest";
import { screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithQuery } from "@/__tests__/test-utils";
import { DisputeOpenModal } from "@/components/dispute/DisputeOpenModal";

// ========================================
// DisputeOpenModal 테스트 (FE-450)
// TDD: Red → Green → Refactor
// ========================================

describe("DisputeOpenModal", () => {
  const defaultProps = {
    rentalId: 1001,
    isOpen: true,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
  };

  // ── 렌더링 ────────────────────────────────────────────────────

  it("모달이 열리면 제목, 사유 드롭다운, textarea, 제출 버튼이 렌더된다", () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} />);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByLabelText(/분쟁 사유/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/상세 설명/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /분쟁 오픈/i })).toBeInTheDocument();
  });

  it("isOpen=false 이면 모달이 렌더되지 않는다", () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} isOpen={false} />);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("닫기 버튼 클릭 시 onClose가 호출된다", async () => {
    const onClose = vi.fn();
    renderWithQuery(<DisputeOpenModal {...defaultProps} onClose={onClose} />);
    await userEvent.click(screen.getByRole("button", { name: /닫기|취소/i }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  // ── Validation ────────────────────────────────────────────────

  it("description이 빈 문자열이면 제출 버튼이 비활성화된다", () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} />);
    const submitBtn = screen.getByRole("button", { name: /분쟁 오픈/i });
    expect(submitBtn).toBeDisabled();
  });

  it("description이 빈 문자열(0자)이면 제출 버튼이 비활성화된 상태를 유지한다", async () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} />);
    // 아무것도 입력하지 않은 초기 상태
    const submitBtn = screen.getByRole("button", { name: /분쟁 오픈/i });
    expect(submitBtn).toBeDisabled();
  });

  it("description이 1000자 초과이면 에러 메시지가 표시된다", async () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} />);
    const textarea = screen.getByLabelText(/상세 설명/i);
    const longText = "a".repeat(1001);
    await userEvent.type(textarea, longText);
    expect(screen.getByText(/1000자 이내/i)).toBeInTheDocument();
  });

  it("description이 1~1000자 범위이면 제출 버튼이 활성화된다", async () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} />);
    const textarea = screen.getByLabelText(/상세 설명/i);
    await userEvent.type(textarea, "파손 상태가 심각합니다.");
    const submitBtn = screen.getByRole("button", { name: /분쟁 오픈/i });
    expect(submitBtn).not.toBeDisabled();
  });

  // ── 정상 제출 ─────────────────────────────────────────────────

  it("정상 제출 시 onSuccess가 호출된다", async () => {
    const onSuccess = vi.fn();
    // rentalId=9999: stub에 활성 분쟁 없음 → 정상 201 응답
    renderWithQuery(
      <DisputeOpenModal {...defaultProps} rentalId={9999} onSuccess={onSuccess} />
    );

    // 사유 선택
    const select = screen.getByLabelText(/분쟁 사유/i);
    fireEvent.change(select, { target: { value: "DAMAGED" } });

    // 설명 입력
    const textarea = screen.getByLabelText(/상세 설명/i);
    await userEvent.type(textarea, "물건이 파손된 상태로 도착했습니다.");

    // 제출
    await userEvent.click(screen.getByRole("button", { name: /분쟁 오픈/i }));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalledTimes(1);
    });
  });

  // ── 409 분기 ─────────────────────────────────────────────────

  it("409 응답 시 '이미 진행 중인 분쟁' 에러가 표시된다", async () => {
    // rentalId=1001은 STUB_DISPUTES에 OPEN 상태 분쟁이 있음 → 409
    renderWithQuery(<DisputeOpenModal {...defaultProps} rentalId={1001} />);

    const select = screen.getByLabelText(/분쟁 사유/i);
    fireEvent.change(select, { target: { value: "DAMAGED" } });
    const textarea = screen.getByLabelText(/상세 설명/i);
    await userEvent.type(textarea, "파손 상태가 심각합니다.");
    await userEvent.click(screen.getByRole("button", { name: /분쟁 오픈/i }));

    await waitFor(() => {
      expect(screen.getByText(/이미 진행 중인 분쟁/i)).toBeInTheDocument();
    });
  });

  // ── 접근성 ────────────────────────────────────────────────────

  it("모달은 role=dialog와 aria-labelledby 속성을 가진다", () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} />);
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveAttribute("aria-labelledby");
  });

  it("첨부파일 입력은 최대 5장 제한 레이블이 표시된다", () => {
    renderWithQuery(<DisputeOpenModal {...defaultProps} />);
    expect(screen.getByText(/최대 5장/i)).toBeInTheDocument();
  });
});
