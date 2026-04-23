import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CategoryFilter } from "@/components/product/search/CategoryFilter";
import { PriceRangeInput } from "@/components/product/search/PriceRangeInput";
import { RegionDropdown } from "@/components/product/search/RegionDropdown";
import { SortToggle } from "@/components/product/search/SortToggle";
import type { ProductSearchCondition, SortBy, SortDirection } from "@/lib/api/product-search-types";

// ========================================
// CategoryFilter 단위 테스트
// ========================================

describe("CategoryFilter", () => {
  it("전체 카테고리 옵션이 렌더링된다", () => {
    render(
      <CategoryFilter
        selectedCodes={[]}
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("group", { name: "카테고리 필터" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /전자기기/ })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /가구/ })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /스포츠/ })).toBeInTheDocument();
  });

  it("선택된 카테고리가 체크 상태로 표시된다", () => {
    render(
      <CategoryFilter
        selectedCodes={["ELECTRONICS", "SPORTS"]}
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("checkbox", { name: /전자기기/ })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: /스포츠/ })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: /가구/ })).not.toBeChecked();
  });

  it("체크박스 클릭 시 onChange가 호출된다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <CategoryFilter
        selectedCodes={[]}
        onChange={onChange}
      />
    );
    await user.click(screen.getByRole("checkbox", { name: /전자기기/ }));
    expect(onChange).toHaveBeenCalledWith(["ELECTRONICS"]);
  });

  it("이미 선택된 카테고리를 클릭하면 선택 해제된다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <CategoryFilter
        selectedCodes={["ELECTRONICS"]}
        onChange={onChange}
      />
    );
    await user.click(screen.getByRole("checkbox", { name: /전자기기/ }));
    expect(onChange).toHaveBeenCalledWith([]);
  });

  it("다중 카테고리 선택이 동작한다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <CategoryFilter
        selectedCodes={["ELECTRONICS"]}
        onChange={onChange}
      />
    );
    await user.click(screen.getByRole("checkbox", { name: /가구/ }));
    expect(onChange).toHaveBeenCalledWith(["ELECTRONICS", "FURNITURE"]);
  });

  it("각 카테고리 chip이 aria-label을 가진다", () => {
    render(
      <CategoryFilter
        selectedCodes={[]}
        onChange={vi.fn()}
      />
    );
    const checkboxes = screen.getAllByRole("checkbox");
    checkboxes.forEach((cb) => {
      expect(cb).toHaveAttribute("aria-label");
    });
  });

  it("키보드로 카테고리를 선택할 수 있다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <CategoryFilter
        selectedCodes={[]}
        onChange={onChange}
      />
    );
    const checkbox = screen.getByRole("checkbox", { name: /전자기기/ });
    checkbox.focus();
    await user.keyboard(" ");
    expect(onChange).toHaveBeenCalledWith(["ELECTRONICS"]);
  });
});

// ========================================
// PriceRangeInput 단위 테스트
// ========================================

describe("PriceRangeInput", () => {
  it("최소/최대 가격 입력 필드가 렌더링된다", () => {
    render(
      <PriceRangeInput
        minPrice={undefined}
        maxPrice={undefined}
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("spinbutton", { name: /최소 가격/ })).toBeInTheDocument();
    expect(screen.getByRole("spinbutton", { name: /최대 가격/ })).toBeInTheDocument();
  });

  it("초기값이 입력 필드에 표시된다", () => {
    render(
      <PriceRangeInput
        minPrice={10000}
        maxPrice={50000}
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("spinbutton", { name: /최소 가격/ })).toHaveValue(10000);
    expect(screen.getByRole("spinbutton", { name: /최대 가격/ })).toHaveValue(50000);
  });

  it("최소 가격 입력 시 onChange가 호출된다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <PriceRangeInput
        minPrice={undefined}
        maxPrice={undefined}
        onChange={onChange}
      />
    );
    const minInput = screen.getByRole("spinbutton", { name: /최소 가격/ });
    await user.clear(minInput);
    await user.type(minInput, "5000");
    // debounce 300ms 후 onChange 호출 — 즉시가 아니라 debounce 완료 후 확인
    await waitFor(() => {
      expect(onChange).toHaveBeenCalled();
    }, { timeout: 500 });
  });

  it("최대 가격이 최소 가격보다 작으면 오류 메시지가 표시된다", async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <PriceRangeInput
        minPrice={50000}
        maxPrice={10000}
        onChange={vi.fn()}
      />
    );
    rerender(
      <PriceRangeInput
        minPrice={50000}
        maxPrice={10000}
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("alert")).toBeInTheDocument();
  });
});

// ========================================
// RegionDropdown 단위 테스트
// ========================================

describe("RegionDropdown", () => {
  it("지역 드롭다운이 렌더링된다", () => {
    render(
      <RegionDropdown
        value={undefined}
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("combobox", { name: /지역/ })).toBeInTheDocument();
  });

  it("지역 옵션 목록이 존재한다", () => {
    render(
      <RegionDropdown
        value={undefined}
        onChange={vi.fn()}
      />
    );
    const select = screen.getByRole("combobox", { name: /지역/ });
    // 전체 + 지역 옵션
    expect(select.querySelectorAll("option").length).toBeGreaterThan(1);
  });

  it("선택된 지역이 드롭다운에 반영된다", () => {
    render(
      <RegionDropdown
        value="SEOUL_GANGNAM"
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("combobox", { name: /지역/ })).toHaveValue("SEOUL_GANGNAM");
  });

  it("지역 변경 시 onChange가 호출된다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <RegionDropdown
        value={undefined}
        onChange={onChange}
      />
    );
    await user.selectOptions(
      screen.getByRole("combobox", { name: /지역/ }),
      "SEOUL_GANGNAM"
    );
    expect(onChange).toHaveBeenCalledWith("SEOUL_GANGNAM");
  });
});

// ========================================
// SortToggle 단위 테스트
// ========================================

describe("SortToggle", () => {
  it("5종 정렬 버튼이 모두 렌더링된다", () => {
    render(
      <SortToggle
        sortBy="CREATED_AT"
        sortDirection="DESC"
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("button", { name: /최신/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /이름/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /보증금/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /평점/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /인기/ })).toBeInTheDocument();
  });

  it("현재 정렬 버튼이 활성화 상태로 표시된다", () => {
    render(
      <SortToggle
        sortBy="CREATED_AT"
        sortDirection="DESC"
        onChange={vi.fn()}
      />
    );
    expect(screen.getByRole("button", { name: /최신/ })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: /이름/ })).toHaveAttribute("aria-pressed", "false");
  });

  it("정렬 버튼 클릭 시 onChange가 호출된다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <SortToggle
        sortBy="CREATED_AT"
        sortDirection="DESC"
        onChange={onChange}
      />
    );
    await user.click(screen.getByRole("button", { name: /평점/ }));
    expect(onChange).toHaveBeenCalledWith({ sortBy: "RATING_AVG", sortDirection: "DESC" });
  });

  it("같은 정렬 버튼 재클릭 시 방향이 토글된다", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <SortToggle
        sortBy="DEPOSIT_AMOUNT"
        sortDirection="ASC"
        onChange={onChange}
      />
    );
    await user.click(screen.getByRole("button", { name: /보증금/ }));
    expect(onChange).toHaveBeenCalledWith({ sortBy: "DEPOSIT_AMOUNT", sortDirection: "DESC" });
  });

  it("각 정렬 버튼에 aria-pressed 속성이 있다", () => {
    render(
      <SortToggle
        sortBy="CREATED_AT"
        sortDirection="DESC"
        onChange={vi.fn()}
      />
    );
    const buttons = screen.getAllByRole("button");
    buttons.forEach((btn) => {
      expect(btn).toHaveAttribute("aria-pressed");
    });
  });
});
