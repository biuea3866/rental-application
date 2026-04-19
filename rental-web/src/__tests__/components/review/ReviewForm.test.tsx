import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ReviewForm } from "@/components/review/ReviewForm";

// ========================================
// ReviewForm 컴포넌트 테스트
// RC-FE-317
// ========================================

describe("ReviewForm", () => {
  it("폼이 렌더링되어야 한다", () => {
    render(<ReviewForm onSubmit={vi.fn()} />);
    expect(screen.getByTestId("review-form")).toBeInTheDocument();
  });

  it("초기 상태에서 제출 버튼이 비활성화되어야 한다", () => {
    render(<ReviewForm onSubmit={vi.fn()} />);
    const submitBtn = screen.getByTestId("review-submit-button");
    expect(submitBtn).toBeDisabled();
  });

  it("별점과 내용을 입력하면 제출 버튼이 활성화되어야 한다", async () => {
    render(<ReviewForm onSubmit={vi.fn()} />);

    fireEvent.click(screen.getByTestId("star-4"));
    fireEvent.change(screen.getByTestId("review-content-input"), {
      target: { value: "좋은 제품이었습니다. 다음에도 이용하고 싶습니다." },
    });

    await waitFor(() => {
      expect(screen.getByTestId("review-submit-button")).not.toBeDisabled();
    });
  });

  it("내용이 10자 미만일 때 제출 버튼이 비활성화되어야 한다", async () => {
    render(<ReviewForm onSubmit={vi.fn()} />);

    fireEvent.click(screen.getByTestId("star-3"));
    fireEvent.change(screen.getByTestId("review-content-input"), {
      target: { value: "짧음" },
    });

    await waitFor(() => {
      expect(screen.getByTestId("review-submit-button")).toBeDisabled();
    });
  });

  it("폼 제출 시 onSubmit이 올바른 데이터로 호출되어야 한다", async () => {
    const onSubmit = vi.fn();
    render(<ReviewForm onSubmit={onSubmit} />);

    fireEvent.click(screen.getByTestId("star-5"));
    fireEvent.change(screen.getByTestId("review-content-input"), {
      target: { value: "정말 만족스러운 대여 경험이었습니다!" },
    });

    await waitFor(() => {
      expect(screen.getByTestId("review-submit-button")).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId("review-submit-button"));

    expect(onSubmit).toHaveBeenCalledWith({
      rating: 5,
      content: "정말 만족스러운 대여 경험이었습니다!",
    });
  });

  it("isSubmitting이 true일 때 제출 중 텍스트가 표시되어야 한다", () => {
    render(<ReviewForm onSubmit={vi.fn()} isSubmitting={true} />);
    expect(screen.getByTestId("review-submit-button")).toHaveTextContent(
      "제출 중..."
    );
  });
});
