import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { RentalStatusTimeline } from "@/components/rental/RentalStatusTimeline";

// ========================================
// RentalStatusTimeline 테스트
// ========================================

const DATES = {
  requestedAt: "2026-04-14T10:00:00Z",
  approvedAt: "2026-04-15T09:00:00Z",
  paidAt: "2026-04-16T10:30:00Z",
  startedAt: "2026-04-17T09:00:00Z",
  returnedAt: "2026-04-22T15:00:00Z",
};

describe("RentalStatusTimeline", () => {
  it("타임라인 컨테이너가 렌더링되어야 한다", () => {
    render(
      <RentalStatusTimeline status="REQUESTED" dates={{ requestedAt: DATES.requestedAt }} />
    );
    expect(screen.getByTestId("rental-status-timeline")).toBeInTheDocument();
  });

  it("REQUESTED 상태에서 신청 스텝이 표시되어야 한다", () => {
    render(
      <RentalStatusTimeline status="REQUESTED" dates={{ requestedAt: DATES.requestedAt }} />
    );
    expect(screen.getByTestId("timeline-step-REQUESTED")).toBeInTheDocument();
  });

  it("APPROVED 상태에서 신청/승인 스텝이 모두 표시되어야 한다", () => {
    render(
      <RentalStatusTimeline
        status="APPROVED"
        dates={{ requestedAt: DATES.requestedAt, approvedAt: DATES.approvedAt }}
      />
    );
    expect(screen.getByTestId("timeline-step-REQUESTED")).toBeInTheDocument();
    expect(screen.getByTestId("timeline-step-APPROVED")).toBeInTheDocument();
  });

  it("PAID 상태에서 결제 스텝이 표시되어야 한다", () => {
    render(
      <RentalStatusTimeline
        status="PAID"
        dates={{
          requestedAt: DATES.requestedAt,
          approvedAt: DATES.approvedAt,
          paidAt: DATES.paidAt,
        }}
      />
    );
    expect(screen.getByTestId("timeline-step-PAID")).toBeInTheDocument();
  });

  it("IN_USE 상태에서 대여 중 스텝이 표시되어야 한다", () => {
    render(
      <RentalStatusTimeline
        status="IN_USE"
        dates={{
          requestedAt: DATES.requestedAt,
          approvedAt: DATES.approvedAt,
          paidAt: DATES.paidAt,
          startedAt: DATES.startedAt,
        }}
      />
    );
    expect(screen.getByTestId("timeline-step-IN_USE")).toBeInTheDocument();
  });

  it("RETURNED 상태에서 반납 스텝이 표시되어야 한다", () => {
    render(
      <RentalStatusTimeline status="RETURNED" dates={DATES} />
    );
    expect(screen.getByTestId("timeline-step-RETURNED")).toBeInTheDocument();
  });

  it("CANCELLED 상태에서 '취소됨' 표시가 나타나야 한다", () => {
    render(
      <RentalStatusTimeline
        status="CANCELLED"
        dates={{ requestedAt: DATES.requestedAt, cancelledAt: "2026-04-14T12:00:00Z" }}
      />
    );
    expect(screen.getByTestId("timeline-cancelled")).toBeInTheDocument();
    expect(screen.getByText("취소됨")).toBeInTheDocument();
  });

  it("CANCELLED 상태에서 일반 타임라인 스텝이 표시되지 않아야 한다", () => {
    render(
      <RentalStatusTimeline
        status="CANCELLED"
        dates={{ requestedAt: DATES.requestedAt, cancelledAt: "2026-04-14T12:00:00Z" }}
      />
    );
    expect(screen.queryByTestId("timeline-step-REQUESTED")).not.toBeInTheDocument();
  });
});
