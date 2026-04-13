import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ProductGrid } from "@/components/product/ProductGrid";
import { STUB_PRODUCTS } from "@/mocks/products";

// ========================================
// ProductGrid 테스트
// ========================================

describe("ProductGrid", () => {
  it("상품 목록이 정상 렌더링되어야 한다", () => {
    render(<ProductGrid products={STUB_PRODUCTS} />);

    expect(screen.getByTestId("product-grid")).toBeInTheDocument();
    // 각 상품 제목이 출력되는지 확인 (최소 2개)
    const links = screen.getAllByRole("link");
    expect(links.length).toBeGreaterThanOrEqual(1);
  });

  it("상품이 없을 때 빈 상태 메시지를 표시해야 한다", () => {
    render(<ProductGrid products={[]} />);

    expect(screen.getByTestId("product-grid-empty")).toBeInTheDocument();
    expect(screen.getByText("검색 결과가 없습니다")).toBeInTheDocument();
  });

  it("로딩 중일 때 스켈레톤을 표시해야 한다", () => {
    render(<ProductGrid products={[]} isLoading />);

    expect(screen.getByTestId("product-grid-skeleton")).toBeInTheDocument();
    const skeletons = screen.getAllByTestId("product-skeleton");
    expect(skeletons.length).toBe(8);
  });

  it("다음 페이지가 있을 때 더보기 버튼을 표시해야 한다", () => {
    render(
      <ProductGrid
        products={STUB_PRODUCTS}
        hasNextPage
        onLoadMore={vi.fn()}
        useInfiniteScroll={false}
      />
    );

    expect(screen.getByTestId("load-more-button")).toBeInTheDocument();
    expect(screen.getByText("더 보기")).toBeInTheDocument();
  });

  it("다음 페이지가 없으면 더보기 버튼을 표시하지 않아야 한다", () => {
    render(
      <ProductGrid
        products={STUB_PRODUCTS}
        hasNextPage={false}
        onLoadMore={vi.fn()}
        useInfiniteScroll={false}
      />
    );

    expect(screen.queryByTestId("load-more-button")).not.toBeInTheDocument();
  });

  it("더보기 버튼 클릭 시 onLoadMore 콜백이 호출되어야 한다", async () => {
    const onLoadMore = vi.fn();
    const { user } = await import("@testing-library/user-event").then((m) => ({
      user: m.default.setup(),
    }));

    render(
      <ProductGrid
        products={STUB_PRODUCTS}
        hasNextPage
        onLoadMore={onLoadMore}
        useInfiniteScroll={false}
      />
    );

    await user.click(screen.getByTestId("load-more-button"));
    expect(onLoadMore).toHaveBeenCalledOnce();
  });

  it("isFetchingNextPage가 true이면 더보기 버튼에 로딩 텍스트가 표시되어야 한다", () => {
    render(
      <ProductGrid
        products={STUB_PRODUCTS}
        hasNextPage
        isFetchingNextPage
        onLoadMore={vi.fn()}
        useInfiniteScroll={false}
      />
    );

    expect(screen.getByText("불러오는 중...")).toBeInTheDocument();
  });

  it("각 상품 카드에 상품 제목이 표시되어야 한다", () => {
    render(<ProductGrid products={[STUB_PRODUCTS[0]]} />);

    expect(
      screen.getByText("소니 A7C II 미러리스 카메라")
    ).toBeInTheDocument();
  });

  it("각 상품 카드가 상세 페이지 링크를 가져야 한다", () => {
    render(<ProductGrid products={[STUB_PRODUCTS[0]]} />);

    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("href", "/products/prod-001");
  });
});
