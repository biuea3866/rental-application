import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { StarRating } from "@/components/review/StarRating";

// ========================================
// StarRating 컴포넌트 테스트
// RC-FE-317
// ========================================

describe("StarRating", () => {
  it("5개의 별이 렌더링되어야 한다", () => {
    render(<StarRating value={0} />);
    for (let i = 1; i <= 5; i++) {
      expect(screen.getByTestId(`star-${i}`)).toBeInTheDocument();
    }
  });

  it("value에 따라 별이 채워져야 한다", () => {
    render(<StarRating value={3} />);
    const starRating = screen.getByTestId("star-rating");
    expect(starRating).toBeInTheDocument();
  });

  it("readonly가 아닐 때 별 클릭 시 onChange가 호출되어야 한다", () => {
    const onChange = vi.fn();
    render(<StarRating value={0} onChange={onChange} />);

    fireEvent.click(screen.getByTestId("star-4"));
    expect(onChange).toHaveBeenCalledWith(4);
  });

  it("readonly일 때 별 클릭 시 onChange가 호출되지 않아야 한다", () => {
    const onChange = vi.fn();
    render(<StarRating value={3} onChange={onChange} readonly />);

    fireEvent.click(screen.getByTestId("star-5"));
    expect(onChange).not.toHaveBeenCalled();
  });

  it("size prop에 따라 올바른 크기 클래스가 적용되어야 한다", () => {
    render(<StarRating value={0} size="lg" />);
    const star = screen.getByTestId("star-1");
    expect(star.className).toContain("w-8");
  });
});
