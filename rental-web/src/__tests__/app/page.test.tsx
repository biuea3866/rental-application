import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import HomePage from "@/app/page";

describe("HomePage", () => {
  it("메인 타이틀이 렌더링되어야 한다", () => {
    render(<HomePage />);

    expect(screen.getByText("Rental Commerce")).toBeInTheDocument();
  });

  it("플랫폼 설명이 표시되어야 한다", () => {
    render(<HomePage />);

    expect(
      screen.getByText("물건을 빌려주고 빌리는 대여 커머스 플랫폼")
    ).toBeInTheDocument();
  });

  it("등록자 카드가 렌더링되어야 한다", () => {
    render(<HomePage />);

    expect(screen.getByText("등록자 (Lender)")).toBeInTheDocument();
    expect(screen.getByText("등록자로 시작하기")).toBeInTheDocument();
  });

  it("대여자 카드가 렌더링되어야 한다", () => {
    render(<HomePage />);

    expect(screen.getByText("대여자 (Renter)")).toBeInTheDocument();
    expect(screen.getByText("대여자로 시작하기")).toBeInTheDocument();
  });

  it("로그인 페이지로 향하는 링크가 있어야 한다", () => {
    render(<HomePage />);

    const links = screen.getAllByRole("link");
    const loginLinks = links.filter(
      (link) => link.getAttribute("href") === "/login"
    );
    expect(loginLinks).toHaveLength(2);
  });
});
