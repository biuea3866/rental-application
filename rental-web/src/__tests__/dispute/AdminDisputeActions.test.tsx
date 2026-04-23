import { describe, it, expect, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithQuery } from "@/__tests__/test-utils";
import { AdminDisputeActions } from "@/components/dispute/AdminDisputeActions";
import type { DisputeResult } from "@/lib/api/types";

// ========================================
// AdminDisputeActions 테스트 (FE-451)
// 검토 시작, 해결 액션 버튼
// ========================================

const openDispute: DisputeResult = {
  id: 101,
  rentalId: 1001,
  openerId: 11,
  reason: "DAMAGED",
  description: "파손 상태가 심각합니다.",
  status: "OPEN",
  attachmentUrls: ["https://example.com/img1.jpg"],
  createdAt: "2026-04-20T10:00:00+09:00",
};

const underReviewDispute: DisputeResult = {
  ...openDispute,
  id: 102,
  status: "UNDER_REVIEW",
};

const resolvedDispute: DisputeResult = {
  ...openDispute,
  id: 103,
  status: "RESOLVED_REFUND",
  refundAmount: 30000,
  resolvedAt: "2026-04-21T10:00:00+09:00",
};

describe("AdminDisputeActions", () => {
  // ── OPEN 상태 ─────────────────────────────────────────────────

  it("OPEN 상태에서 '검토 시작' 버튼이 렌더된다", () => {
    renderWithQuery(
      <AdminDisputeActions dispute={openDispute} onUpdated={vi.fn()} />
    );
    expect(screen.getByRole("button", { name: /검토 시작/i })).toBeInTheDocument();
  });

  it("OPEN 상태에서 '해결' 버튼은 렌더되지 않는다", () => {
    renderWithQuery(
      <AdminDisputeActions dispute={openDispute} onUpdated={vi.fn()} />
    );
    expect(
      screen.queryByRole("button", { name: /^해결$/ })
    ).not.toBeInTheDocument();
  });

  it("'검토 시작' 클릭 후 onUpdated가 호출된다", async () => {
    const onUpdated = vi.fn();
    renderWithQuery(
      <AdminDisputeActions dispute={openDispute} onUpdated={onUpdated} />
    );

    await userEvent.click(screen.getByRole("button", { name: /검토 시작/i }));

    await waitFor(() => {
      expect(onUpdated).toHaveBeenCalledTimes(1);
    });
  });

  // ── UNDER_REVIEW 상태 ─────────────────────────────────────────

  it("UNDER_REVIEW 상태에서 '해결' 버튼이 렌더된다", () => {
    renderWithQuery(
      <AdminDisputeActions dispute={underReviewDispute} onUpdated={vi.fn()} />
    );
    expect(screen.getByRole("button", { name: /해결/i })).toBeInTheDocument();
  });

  it("UNDER_REVIEW 상태에서 '검토 시작' 버튼이 렌더되지 않는다", () => {
    renderWithQuery(
      <AdminDisputeActions dispute={underReviewDispute} onUpdated={vi.fn()} />
    );
    expect(
      screen.queryByRole("button", { name: /검토 시작/i })
    ).not.toBeInTheDocument();
  });

  // ── 최종 해결된 상태 ───────────────────────────────────────────

  it("RESOLVED_REFUND 상태에서 액션 버튼이 렌더되지 않는다", () => {
    renderWithQuery(
      <AdminDisputeActions dispute={resolvedDispute} onUpdated={vi.fn()} />
    );
    expect(
      screen.queryByRole("button", { name: /검토 시작|^해결$/ })
    ).not.toBeInTheDocument();
  });

  // ── PG pending 뱃지 ───────────────────────────────────────────

  it("RESOLVED_REFUND 상태에서 'PG pending' 뱃지가 표시된다", () => {
    renderWithQuery(
      <AdminDisputeActions dispute={resolvedDispute} onUpdated={vi.fn()} />
    );
    expect(screen.getByText(/PG pending/i)).toBeInTheDocument();
  });

  it("RESOLVED_PARTIAL 상태에서 'PG pending' 뱃지가 표시된다", () => {
    const partialDispute: DisputeResult = {
      ...resolvedDispute,
      status: "RESOLVED_PARTIAL",
    };
    renderWithQuery(
      <AdminDisputeActions dispute={partialDispute} onUpdated={vi.fn()} />
    );
    expect(screen.getByText(/PG pending/i)).toBeInTheDocument();
  });

  it("RESOLVED_REJECTED 상태에서 'PG pending' 뱃지가 표시되지 않는다", () => {
    const rejectedDispute: DisputeResult = {
      ...resolvedDispute,
      status: "RESOLVED_REJECTED",
    };
    renderWithQuery(
      <AdminDisputeActions dispute={rejectedDispute} onUpdated={vi.fn()} />
    );
    expect(screen.queryByText(/PG pending/i)).not.toBeInTheDocument();
  });
});
