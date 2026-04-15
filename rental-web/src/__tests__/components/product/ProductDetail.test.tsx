import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import { ProductDetail } from "@/components/product/ProductDetail";
import { renderWithQuery } from "@/__tests__/test-utils";
import { STUB_PRODUCTS } from "@/mocks/products";
import { getDailyPrice } from "@/lib/api/types";

// ========================================
// ProductDetail 테스트 (BE 스키마 기준)
// ========================================

// APPROVED 상품 (대여 가능)
const AVAILABLE_PRODUCT = STUB_PRODUCTS.find((p) => p.status === "APPROVED")!;
// SUSPENDED 상품 (대여 불가)
const SUSPENDED_PRODUCT = STUB_PRODUCTS.find((p) => p.status === "SUSPENDED")!;

describe("ProductDetail", () => {
  it("상품 제목이 렌더링되어야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    expect(
      screen.getByText(AVAILABLE_PRODUCT.name!)
    ).toBeInTheDocument();
  });

  it("상품 설명이 렌더링되어야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    expect(
      screen.getByText(AVAILABLE_PRODUCT.description!)
    ).toBeInTheDocument();
  });

  it("가격 정보가 정상 표시되어야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    const dailyPrice = getDailyPrice(AVAILABLE_PRODUCT);
    expect(dailyPrice).not.toBeNull();
    const priceText = `${dailyPrice!.toLocaleString()}원`;
    expect(screen.getByText(priceText)).toBeInTheDocument();
  });

  it("보증금 정보가 표시되어야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    const depositText = `${AVAILABLE_PRODUCT.depositAmount!.toLocaleString()}원`;
    expect(screen.getByText(depositText)).toBeInTheDocument();
  });

  it("대여 가능 상품에는 '대여 신청하기' 버튼이 활성화되어야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    const button = screen.getByTestId("rental-request-button");
    expect(button).toBeInTheDocument();
    expect(button).not.toBeDisabled();
    expect(button).toHaveTextContent("대여 신청하기");
  });

  it("대여 불가 상품에는 '대여 불가' 버튼이 비활성화되어야 한다", () => {
    renderWithQuery(<ProductDetail product={SUSPENDED_PRODUCT} />);

    const button = screen.getByTestId("rental-request-button");
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent("대여 불가");
  });

  it("상태 뱃지가 올바르게 표시되어야 한다 - 대여 가능", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    expect(screen.getByText("대여 가능")).toBeInTheDocument();
  });

  it("상태 뱃지가 올바르게 표시되어야 한다 - 정지", () => {
    renderWithQuery(<ProductDetail product={SUSPENDED_PRODUCT} />);

    expect(screen.getByText("정지")).toBeInTheDocument();
  });

  it("'목록으로' 링크가 /products로 향해야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    const link = screen.getByText("목록으로").closest("a");
    expect(link).toHaveAttribute("href", "/products");
  });

  it("이미지 갤러리가 렌더링되어야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    expect(screen.getByTestId("image-gallery")).toBeInTheDocument();
  });

  it("product-detail data-testid가 존재해야 한다", () => {
    renderWithQuery(<ProductDetail product={AVAILABLE_PRODUCT} />);

    expect(screen.getByTestId("product-detail")).toBeInTheDocument();
  });
});
