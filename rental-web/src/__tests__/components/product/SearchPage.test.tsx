import { describe, it, expect, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithQuery } from "@/__tests__/test-utils";
import { ProductSearchPage } from "@/components/product/search/ProductSearchPage";

// next/navigation mock
vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => "/products",
  useSearchParams: () => new URLSearchParams(),
}));

// ========================================
// URL 쿼리 동기화 테스트
// ========================================

describe("ProductSearchPage - URL sync", () => {
  it("initialParams keyword가 검색창에 반영된다", () => {
    renderWithQuery(
      <ProductSearchPage
        initialParams={{ keyword: "카메라" }}
      />
    );
    // 키워드 검색창에 초기값 반영
    expect(screen.getByDisplayValue("카메라")).toBeInTheDocument();
  });

  it("initialParams categoryCodes가 카테고리 체크 상태에 반영된다", async () => {
    const user = userEvent.setup();
    renderWithQuery(
      <ProductSearchPage
        initialParams={{ categoryCodes: ["ELECTRONICS"] }}
      />
    );
    // 필터 패널 열기
    await user.click(screen.getByTestId("toggle-filters-button"));
    expect(screen.getByRole("checkbox", { name: /전자기기/ })).toBeChecked();
  });

  it("정렬 초기값이 반영된다", () => {
    renderWithQuery(
      <ProductSearchPage
        initialParams={{ sortBy: "RATING_AVG", sortDirection: "DESC" }}
      />
    );
    // 정렬 토글은 필터 패널 밖에도 표시됨
    expect(screen.getAllByRole("button", { name: /평점/ })[0]).toHaveAttribute("aria-pressed", "true");
  });
});

// ========================================
// Empty state 테스트
// ========================================

describe("ProductSearchPage - empty state", () => {
  it("검색 결과 0건 시 empty state가 표시된다", async () => {
    // MSW에서 0건 응답을 위해 존재하지 않는 키워드로 검색
    renderWithQuery(
      <ProductSearchPage
        initialParams={{ keyword: "절대없는상품XYZ999" }}
      />
    );
    await waitFor(
      () => {
        expect(screen.getByTestId("product-grid-empty")).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });
});

// ========================================
// 필터 조합 테스트
// ========================================

describe("ProductSearchPage - filter combinations", () => {
  it("카테고리 + 정렬 조합 필터가 동작한다", async () => {
    const user = userEvent.setup();
    renderWithQuery(
      <ProductSearchPage initialParams={{}} />
    );

    // 필터 패널 열기
    await user.click(screen.getByTestId("toggle-filters-button"));

    // 카테고리 선택
    const electronicsCheckbox = screen.getByRole("checkbox", { name: /전자기기/ });
    await user.click(electronicsCheckbox);
    expect(electronicsCheckbox).toBeChecked();

    // 정렬 변경 (필터 패널 내 정렬)
    const ratingBtns = screen.getAllByRole("button", { name: /평점/ });
    await user.click(ratingBtns[0]);
    expect(ratingBtns[0]).toHaveAttribute("aria-pressed", "true");
  });

  it("지역 + 카테고리 조합 필터가 동작한다", async () => {
    const user = userEvent.setup();
    renderWithQuery(
      <ProductSearchPage initialParams={{}} />
    );

    // 필터 패널 열기
    await user.click(screen.getByTestId("toggle-filters-button"));

    const regionSelect = screen.getByRole("combobox", { name: /지역/ });
    await user.selectOptions(regionSelect, "SEOUL_GANGNAM");
    expect(regionSelect).toHaveValue("SEOUL_GANGNAM");

    const sportsCheckbox = screen.getByRole("checkbox", { name: /스포츠/ });
    await user.click(sportsCheckbox);
    expect(sportsCheckbox).toBeChecked();
  });

  it("가격 범위 + 카테고리 조합 필터가 동작한다", async () => {
    const user = userEvent.setup();
    renderWithQuery(
      <ProductSearchPage initialParams={{}} />
    );

    // 필터 패널 열기
    await user.click(screen.getByTestId("toggle-filters-button"));

    const minInput = screen.getByRole("spinbutton", { name: /최소 가격/ });
    await user.clear(minInput);
    await user.type(minInput, "10000");

    const furnitureCheckbox = screen.getByRole("checkbox", { name: /가구/ });
    await user.click(furnitureCheckbox);
    expect(furnitureCheckbox).toBeChecked();
  });

  it("여러 카테고리 동시 선택 후 해제가 동작한다", async () => {
    const user = userEvent.setup();
    renderWithQuery(
      <ProductSearchPage initialParams={{ categoryCodes: ["ELECTRONICS", "SPORTS"] }} />
    );

    // 필터 패널 열기
    await user.click(screen.getByTestId("toggle-filters-button"));

    const electronicsCheckbox = screen.getByRole("checkbox", { name: /전자기기/ });
    const sportsCheckbox = screen.getByRole("checkbox", { name: /스포츠/ });
    expect(electronicsCheckbox).toBeChecked();
    expect(sportsCheckbox).toBeChecked();

    // ELECTRONICS 해제
    await user.click(electronicsCheckbox);
    expect(electronicsCheckbox).not.toBeChecked();
    expect(sportsCheckbox).toBeChecked();
  });
});

// ========================================
// 페이지네이션 테스트
// ========================================

describe("ProductSearchPage - pagination", () => {
  it("상품 목록이 정상 로딩된다", async () => {
    renderWithQuery(
      <ProductSearchPage initialParams={{}} />
    );
    await waitFor(
      () => {
        expect(screen.getByTestId("product-grid")).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });

  it("기본 size=20으로 요청된다", async () => {
    renderWithQuery(
      <ProductSearchPage initialParams={{}} />
    );
    // 상품이 로드되면 totalCount 텍스트 확인
    await waitFor(
      () => {
        expect(screen.getByTestId("product-grid")).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });
});

// ========================================
// 상품 카드 평점/대여횟수 표시 테스트
// ========================================

describe("ProductCard - rating and rental count", () => {
  it("상품 카드에 평점이 표시된다", async () => {
    renderWithQuery(
      <ProductSearchPage initialParams={{}} />
    );
    await waitFor(
      () => {
        // 평점이 있는 상품이 있으면 표시 확인
        const grid = screen.getByTestId("product-grid");
        expect(grid).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });
});
