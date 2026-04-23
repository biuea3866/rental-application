import { describe, it, expect, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithQuery } from "@/__tests__/test-utils";
import { AdminDisputeResolveModal } from "@/components/dispute/AdminDisputeResolveModal";
import type { DisputeResult } from "@/lib/api/types";

// ========================================
// AdminDisputeResolveModal 테스트 (FE-451)
// TDD: 부분환불 금액 모달, 관리자 액션 실패 롤백
// ========================================

const underReviewDispute: DisputeResult = {
  id: 102,
  rentalId: 1002,
  openerId: 11,
  reason: "LATE_RETURN",
  description: "반납 기한보다 5일 늦게 반납되었습니다.",
  status: "UNDER_REVIEW",
  attachmentUrls: [],
  createdAt: "2026-04-18T09:00:00+09:00",
};

describe("AdminDisputeResolveModal", () => {
  const defaultProps = {
    dispute: underReviewDispute,
    isOpen: true,
    onClose: vi.fn(),
    onResolved: vi.fn(),
  };

  // ── 렌더링 ────────────────────────────────────────────────────

  it("모달이 열리면 해결 유형 선택 버튼 3개가 렌더된다", () => {
    renderWithQuery(<AdminDisputeResolveModal {...defaultProps} />);
    expect(screen.getByRole("button", { name: /전체 환불/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /부분 환불/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /기각/i })).toBeInTheDocument();
  });

  it("isOpen=false이면 모달이 렌더되지 않는다", () => {
    renderWithQuery(<AdminDisputeResolveModal {...defaultProps} isOpen={false} />);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("모달은 role=dialog와 aria-labelledby 속성을 가진다", () => {
    renderWithQuery(<AdminDisputeResolveModal {...defaultProps} />);
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveAttribute("aria-labelledby");
  });

  // ── 부분환불 금액 입력 ─────────────────────────────────────────

  it("'부분 환불' 선택 시 금액 입력 필드가 나타난다", async () => {
    renderWithQuery(<AdminDisputeResolveModal {...defaultProps} />);
    await userEvent.click(screen.getByRole("button", { name: /부분 환불/i }));
    expect(screen.getByLabelText(/환불 금액/i)).toBeInTheDocument();
  });

  it("'부분 환불' 에서 금액 미입력 시 확인 버튼이 비활성화된다", async () => {
    renderWithQuery(<AdminDisputeResolveModal {...defaultProps} />);
    await userEvent.click(screen.getByRole("button", { name: /부분 환불/i }));
    const confirmBtn = screen.getByRole("button", { name: /확인|해결/i });
    expect(confirmBtn).toBeDisabled();
  });

  it("'부분 환불'에서 금액 입력 후 확인 버튼이 활성화된다", async () => {
    renderWithQuery(<AdminDisputeResolveModal {...defaultProps} />);
    await userEvent.click(screen.getByRole("button", { name: /부분 환불/i }));
    const amountInput = screen.getByLabelText(/환불 금액/i);
    await userEvent.type(amountInput, "50000");
    const confirmBtn = screen.getByRole("button", { name: /확인|해결/i });
    expect(confirmBtn).not.toBeDisabled();
  });

  // ── 전체환불 제출 ─────────────────────────────────────────────

  it("'전체 환불' 선택 후 확인하면 onResolved가 호출된다", async () => {
    const onResolved = vi.fn();
    renderWithQuery(
      <AdminDisputeResolveModal {...defaultProps} onResolved={onResolved} />
    );

    await userEvent.click(screen.getByRole("button", { name: /전체 환불/i }));
    await userEvent.click(screen.getByRole("button", { name: /확인|해결/i }));

    await waitFor(() => {
      expect(onResolved).toHaveBeenCalledTimes(1);
    });
  });

  // ── 기각 제출 ────────────────────────────────────────────────

  it("'기각' 선택 후 확인하면 onResolved가 호출된다", async () => {
    const onResolved = vi.fn();
    renderWithQuery(
      <AdminDisputeResolveModal {...defaultProps} onResolved={onResolved} />
    );

    await userEvent.click(screen.getByRole("button", { name: /기각/i }));
    await userEvent.click(screen.getByRole("button", { name: /확인|해결/i }));

    await waitFor(() => {
      expect(onResolved).toHaveBeenCalledTimes(1);
    });
  });

  // ── 에러 상태에서 롤백 확인 ──────────────────────────────────

  it("OPEN 상태 분쟁(id=101)으로 resolve 요청 시 에러 메시지가 표시된다", async () => {
    const openDispute: DisputeResult = {
      ...underReviewDispute,
      id: 101,
      status: "OPEN",
    };

    renderWithQuery(
      <AdminDisputeResolveModal {...defaultProps} dispute={openDispute} />
    );

    await userEvent.click(screen.getByRole("button", { name: /전체 환불/i }));
    await userEvent.click(screen.getByRole("button", { name: /확인|해결/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeInTheDocument();
    });
  });

  // ── 닫기 ─────────────────────────────────────────────────────

  it("닫기 버튼 클릭 시 onClose가 호출된다", async () => {
    const onClose = vi.fn();
    renderWithQuery(
      <AdminDisputeResolveModal {...defaultProps} onClose={onClose} />
    );
    const closeBtn = screen.getByRole("button", { name: /닫기|취소/i });
    await userEvent.click(closeBtn);
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
