import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { QueryProvider } from "@/providers/query-provider";
import { useQueryClient } from "@tanstack/react-query";

// ========================================
// QueryProvider 테스트
// ========================================

function QueryClientConsumer() {
  const queryClient = useQueryClient();
  const defaultOptions = queryClient.getDefaultOptions();
  return (
    <div>
      <span data-testid="stale-time">
        {String(defaultOptions.queries?.staleTime)}
      </span>
      <span>QueryProvider 작동 중</span>
    </div>
  );
}

describe("QueryProvider", () => {
  it("children을 정상적으로 렌더링해야 한다", () => {
    render(
      <QueryProvider>
        <div>테스트 자식 컴포넌트</div>
      </QueryProvider>
    );

    expect(screen.getByText("테스트 자식 컴포넌트")).toBeInTheDocument();
  });

  it("QueryClient를 자식에게 제공해야 한다", () => {
    render(
      <QueryProvider>
        <QueryClientConsumer />
      </QueryProvider>
    );

    expect(screen.getByText("QueryProvider 작동 중")).toBeInTheDocument();
  });

  it("staleTime이 60초(60000ms)로 설정되어야 한다", () => {
    render(
      <QueryProvider>
        <QueryClientConsumer />
      </QueryProvider>
    );

    expect(screen.getByTestId("stale-time").textContent).toBe("60000");
  });

  it("여러 자식 컴포넌트를 렌더링할 수 있어야 한다", () => {
    render(
      <QueryProvider>
        <div>자식 1</div>
        <div>자식 2</div>
        <div>자식 3</div>
      </QueryProvider>
    );

    expect(screen.getByText("자식 1")).toBeInTheDocument();
    expect(screen.getByText("자식 2")).toBeInTheDocument();
    expect(screen.getByText("자식 3")).toBeInTheDocument();
  });
});
