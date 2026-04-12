import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ProductSearchBar } from "@/components/product/ProductSearchBar";

// ========================================
// ProductSearchBar 테스트
// ========================================

describe("ProductSearchBar", () => {
  it("검색바가 렌더링되어야 한다", () => {
    render(<ProductSearchBar onSearch={vi.fn()} />);

    expect(screen.getByTestId("search-input")).toBeInTheDocument();
    expect(screen.getByTestId("search-button")).toBeInTheDocument();
  });

  it("기본값이 있으면 인풋에 표시되어야 한다", () => {
    render(<ProductSearchBar defaultValue="카메라" onSearch={vi.fn()} />);

    const input = screen.getByTestId("search-input") as HTMLInputElement;
    expect(input.value).toBe("카메라");
  });

  it("placeholder가 정상 표시되어야 한다", () => {
    render(
      <ProductSearchBar
        placeholder="원하는 상품을 검색하세요"
        onSearch={vi.fn()}
      />
    );

    expect(
      screen.getByPlaceholderText("원하는 상품을 검색하세요")
    ).toBeInTheDocument();
  });

  it("검색 버튼 클릭 시 onSearch가 호출되어야 한다", async () => {
    const onSearch = vi.fn();
    const user = userEvent.setup();

    render(<ProductSearchBar onSearch={onSearch} />);

    const input = screen.getByTestId("search-input");
    await user.type(input, "카메라");
    await user.click(screen.getByTestId("search-button"));

    expect(onSearch).toHaveBeenCalledWith("카메라");
  });

  it("Enter 키 입력 시 onSearch가 호출되어야 한다", async () => {
    const onSearch = vi.fn();
    const user = userEvent.setup();

    render(<ProductSearchBar onSearch={onSearch} />);

    const input = screen.getByTestId("search-input");
    await user.type(input, "맥북{Enter}");

    expect(onSearch).toHaveBeenCalledWith("맥북");
  });

  it("초기화 버튼 클릭 시 검색어가 지워지고 onSearch('')가 호출되어야 한다", async () => {
    const onSearch = vi.fn();
    const user = userEvent.setup();

    render(<ProductSearchBar defaultValue="카메라" onSearch={onSearch} />);

    expect(screen.getByTestId("clear-button")).toBeInTheDocument();
    await user.click(screen.getByTestId("clear-button"));

    const input = screen.getByTestId("search-input") as HTMLInputElement;
    expect(input.value).toBe("");
    expect(onSearch).toHaveBeenCalledWith("");
  });

  it("검색어가 없으면 초기화 버튼이 표시되지 않아야 한다", () => {
    render(<ProductSearchBar onSearch={vi.fn()} />);

    expect(screen.queryByTestId("clear-button")).not.toBeInTheDocument();
  });

  it("검색어 입력 시 자동완성 제안이 표시되어야 한다", async () => {
    const user = userEvent.setup();

    render(<ProductSearchBar onSearch={vi.fn()} />);

    const input = screen.getByTestId("search-input");
    await user.type(input, "카");

    await waitFor(() => {
      expect(screen.getByTestId("autocomplete-list")).toBeInTheDocument();
    });
  });

  it("자동완성 항목 선택 시 onSearch가 호출되어야 한다", async () => {
    const onSearch = vi.fn();
    const user = userEvent.setup();

    render(<ProductSearchBar onSearch={onSearch} />);

    const input = screen.getByTestId("search-input");
    await user.type(input, "카");

    await waitFor(() => {
      expect(screen.getByTestId("autocomplete-list")).toBeInTheDocument();
    });

    // 첫 번째 자동완성 항목 선택
    const items = screen.getAllByRole("option");
    expect(items.length).toBeGreaterThan(0);
    fireEvent.mouseDown(items[0]);

    expect(onSearch).toHaveBeenCalled();
  });

  it("앞뒤 공백이 제거된 검색어로 onSearch가 호출되어야 한다", async () => {
    const onSearch = vi.fn();
    const user = userEvent.setup();

    render(<ProductSearchBar onSearch={onSearch} />);

    const input = screen.getByTestId("search-input");
    await user.type(input, "  카메라  {Enter}");

    expect(onSearch).toHaveBeenCalledWith("카메라");
  });
});
