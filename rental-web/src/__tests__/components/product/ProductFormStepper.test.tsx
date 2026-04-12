import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, beforeEach, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ProductFormStepper } from "@/components/product/ProductFormStepper";
import { Step1BasicInfo } from "@/components/product/Step1BasicInfo";
import { Step2Pricing } from "@/components/product/Step2Pricing";
import { useProductFormStore } from "@/stores/product-form-store";

// ========================================
// 헬퍼
// ========================================

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
}

function renderWithQuery(ui: React.ReactElement) {
  const client = makeQueryClient();
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>
  );
}

// ========================================
// ProductFormStepper 테스트
// ========================================

describe("ProductFormStepper", () => {
  it("1단계에서 기본정보가 활성 상태로 표시된다", () => {
    render(<ProductFormStepper currentStep={1} />);

    const step1 = screen.getByTestId("step-indicator-1");
    expect(step1).toHaveAttribute("aria-current", "step");
  });

  it("3단계일 때 1, 2단계는 완료 상태이다", () => {
    render(<ProductFormStepper currentStep={3} />);

    const step1 = screen.getByTestId("step-indicator-1");
    const step2 = screen.getByTestId("step-indicator-2");
    const step3 = screen.getByTestId("step-indicator-3");

    // 완료된 단계는 숫자 대신 체크 아이콘이 들어 있어 숫자가 없음
    expect(step1).not.toHaveTextContent("1");
    expect(step2).not.toHaveTextContent("2");
    // 현재 단계는 숫자가 그대로
    expect(step3).toHaveTextContent("3");
    expect(step3).toHaveAttribute("aria-current", "step");
  });

  it("5단계까지 모든 라벨이 렌더링된다", () => {
    render(<ProductFormStepper currentStep={1} />);

    expect(screen.getByText("기본 정보")).toBeInTheDocument();
    expect(screen.getByText("가격 설정")).toBeInTheDocument();
    expect(screen.getByText("이미지")).toBeInTheDocument();
    expect(screen.getByText("상태 설명")).toBeInTheDocument();
    expect(screen.getByText("최종 확인")).toBeInTheDocument();
  });
});

// ========================================
// Step1BasicInfo 유효성 검사 테스트
// ========================================

describe("Step1BasicInfo — 유효성 검사", () => {
  const user = userEvent.setup();

  beforeEach(() => {
    useProductFormStore.getState().reset();
  });

  it("빈 폼 제출 시 에러 메시지가 표시된다", async () => {
    const onNext = vi.fn();
    renderWithQuery(<Step1BasicInfo onNext={onNext} />);

    const submitBtn = screen.getByRole("button", { name: /다음 단계/i });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/카테고리를 선택해 주세요/i)).toBeInTheDocument();
    });

    expect(onNext).not.toHaveBeenCalled();
  });

  it("상품명이 2자 미만이면 에러가 표시된다", async () => {
    const onNext = vi.fn();
    renderWithQuery(<Step1BasicInfo onNext={onNext} />);

    await user.type(screen.getByLabelText(/상품명/i), "A");
    await user.click(screen.getByRole("button", { name: /다음 단계/i }));

    await waitFor(() => {
      expect(
        screen.getByText(/2자 이상 입력해 주세요/i)
      ).toBeInTheDocument();
    });
  });

  it("올바른 값 입력 후 onNext가 호출된다", async () => {
    const onNext = vi.fn();
    renderWithQuery(<Step1BasicInfo onNext={onNext} />);

    // 카테고리 선택
    await user.selectOptions(
      screen.getByRole("combobox"),
      "ELECTRONICS"
    );

    // 상품명
    await user.type(screen.getByLabelText(/상품명/i), "테스트 카메라");

    // 설명 (10자 이상)
    await user.type(
      screen.getByLabelText(/상품 설명/i),
      "상세한 상품 설명입니다. 카메라 관련 설명."
    );

    // 위치
    await user.type(screen.getByLabelText(/위치/i), "서울 강남구");

    await user.click(screen.getByRole("button", { name: /다음 단계/i }));

    await waitFor(() => {
      expect(onNext).toHaveBeenCalledOnce();
    });
  });
});

// ========================================
// Step2Pricing 유효성 검사 테스트
// ========================================

describe("Step2Pricing — 유효성 검사", () => {
  const user = userEvent.setup();

  beforeEach(() => {
    useProductFormStore.getState().reset();
  });

  it("빈 폼 제출 시 보증금과 일 대여가 에러가 표시된다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step2Pricing onNext={onNext} onPrev={onPrev} />);

    await user.click(screen.getByRole("button", { name: /다음 단계/i }));

    await waitFor(() => {
      expect(screen.getByText(/보증금을 입력해 주세요/i)).toBeInTheDocument();
      expect(screen.getByText(/일 대여가를 입력해 주세요/i)).toBeInTheDocument();
    });

    expect(onNext).not.toHaveBeenCalled();
  });

  it("올바른 값 입력 후 onNext가 호출된다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step2Pricing onNext={onNext} onPrev={onPrev} />);

    await user.type(screen.getByLabelText(/보증금/i), "100000");
    await user.type(screen.getByLabelText(/일 대여가/i), "10000");

    await user.click(screen.getByRole("button", { name: /다음 단계/i }));

    await waitFor(() => {
      expect(onNext).toHaveBeenCalledOnce();
    });
  });

  it("이전 버튼 클릭 시 onPrev가 호출된다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step2Pricing onNext={onNext} onPrev={onPrev} />);

    await user.click(screen.getByRole("button", { name: /이전/i }));

    expect(onPrev).toHaveBeenCalledOnce();
  });
});
