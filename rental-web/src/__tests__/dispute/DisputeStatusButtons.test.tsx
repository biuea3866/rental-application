import { describe, it, expect, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithQuery } from "@/__tests__/test-utils";
import { DisputeStatusButtons } from "@/components/dispute/DisputeStatusButtons";
import type { DisputeResult } from "@/lib/api/types";

// ========================================
// DisputeStatusButtons 테스트 (FE-450)
// 상태별 버튼 표시 테스트
// ========================================

const makeDispute = (overrides: Partial<DisputeResult> = {}): DisputeResult => ({
  id: 101,
  rentalId: 1001,
  openerId: 11,
  reason: "DAMAGED",
  description: "파손 상태가 심각합니다.",
  status: "OPEN",
  attachmentUrls: [],
  createdAt: "2026-04-20T10:00:00+09:00",
  ...overrides,
});

describe("DisputeStatusButtons", () => {
  it("OPEN 상태에서 '취소' 버튼이 렌더된다", () => {
    renderWithQuery(
      <DisputeStatusButtons
        dispute={makeDispute({ status: "OPEN" })}
        onCancelled={vi.fn()}
      />
    );
    expect(screen.getByRole("button", { name: /분쟁 취소/i })).toBeInTheDocument();
  });

  it("UNDER_REVIEW 상태에서 '취소' 버튼이 렌더되지 않는다", () => {
    renderWithQuery(
      <DisputeStatusButtons
        dispute={makeDispute({ status: "UNDER_REVIEW" })}
        onCancelled={vi.fn()}
      />
    );
    expect(screen.queryByRole("button", { name: /분쟁 취소/i })).not.toBeInTheDocument();
  });

  it("RESOLVED_REFUND 상태에서 '취소' 버튼이 렌더되지 않는다", () => {
    renderWithQuery(
      <DisputeStatusButtons
        dispute={makeDispute({ status: "RESOLVED_REFUND" })}
        onCancelled={vi.fn()}
      />
    );
    expect(screen.queryByRole("button", { name: /분쟁 취소/i })).not.toBeInTheDocument();
  });

  it("CANCELLED 상태에서 '취소' 버튼이 렌더되지 않는다", () => {
    renderWithQuery(
      <DisputeStatusButtons
        dispute={makeDispute({ status: "CANCELLED" })}
        onCancelled={vi.fn()}
      />
    );
    expect(screen.queryByRole("button", { name: /분쟁 취소/i })).not.toBeInTheDocument();
  });

  it("'취소' 버튼 클릭 후 onCancelled가 호출된다", async () => {
    const onCancelled = vi.fn();
    renderWithQuery(
      <DisputeStatusButtons
        dispute={makeDispute({ status: "OPEN", id: 101 })}
        onCancelled={onCancelled}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /분쟁 취소/i }));

    await waitFor(() => {
      expect(onCancelled).toHaveBeenCalledTimes(1);
    });
  });
});
