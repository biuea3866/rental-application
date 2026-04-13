import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { LenderProfileSection } from "@/components/mypage/LenderProfileSection";
import type { LenderProfile } from "@/lib/api/types";

// ========================================
// LenderProfileSection 테스트
// ========================================

const fullLenderProfile: LenderProfile = {
  businessName: "테스트 렌탈샵",
  description: "좋은 물건을 합리적인 가격에 빌려드립니다.",
  bankName: "카카오뱅크",
  bankAccount: "3333-01-1234567",
};

describe("LenderProfileSection", () => {
  describe("프로필이 있을 때", () => {
    it("상호명이 렌더링되어야 한다", () => {
      render(<LenderProfileSection lenderProfile={fullLenderProfile} />);

      expect(screen.getByText("테스트 렌탈샵")).toBeInTheDocument();
    });

    it("소개가 렌더링되어야 한다", () => {
      render(<LenderProfileSection lenderProfile={fullLenderProfile} />);

      expect(
        screen.getByText("좋은 물건을 합리적인 가격에 빌려드립니다.")
      ).toBeInTheDocument();
    });

    it("은행명이 렌더링되어야 한다", () => {
      render(<LenderProfileSection lenderProfile={fullLenderProfile} />);

      expect(screen.getByText("카카오뱅크")).toBeInTheDocument();
    });

    it("계좌번호가 렌더링되어야 한다", () => {
      render(<LenderProfileSection lenderProfile={fullLenderProfile} />);

      expect(screen.getByText("3333-01-1234567")).toBeInTheDocument();
    });

    it("'등록자 프로필' 제목이 렌더링되어야 한다", () => {
      render(<LenderProfileSection lenderProfile={fullLenderProfile} />);

      expect(screen.getByText("등록자 프로필")).toBeInTheDocument();
    });
  });

  describe("프로필이 없을 때", () => {
    it("'등록자 프로필이 없습니다' 메시지가 표시되어야 한다", () => {
      render(<LenderProfileSection />);

      expect(screen.getByText("등록자 프로필이 없습니다.")).toBeInTheDocument();
    });

    it("onEdit이 있으면 '프로필 등록' 버튼이 표시되어야 한다", () => {
      render(<LenderProfileSection onEdit={vi.fn()} />);

      expect(screen.getByText("프로필 등록")).toBeInTheDocument();
    });

    it("onEdit이 없으면 '프로필 등록' 버튼이 표시되지 않아야 한다", () => {
      render(<LenderProfileSection />);

      expect(screen.queryByText("프로필 등록")).not.toBeInTheDocument();
    });
  });

  describe("onEdit prop", () => {
    it("onEdit이 있으면 헤더에 '수정' 버튼이 표시되어야 한다", () => {
      render(
        <LenderProfileSection
          lenderProfile={fullLenderProfile}
          onEdit={vi.fn()}
        />
      );

      expect(screen.getByRole("button", { name: "수정" })).toBeInTheDocument();
    });

    it("onEdit이 없으면 헤더에 '수정' 버튼이 표시되지 않아야 한다", () => {
      render(<LenderProfileSection lenderProfile={fullLenderProfile} />);

      expect(screen.queryByRole("button", { name: "수정" })).not.toBeInTheDocument();
    });

    it("'수정' 버튼 클릭 시 onEdit이 호출되어야 한다", async () => {
      const user = userEvent.setup();
      const onEdit = vi.fn();

      render(
        <LenderProfileSection lenderProfile={fullLenderProfile} onEdit={onEdit} />
      );

      await user.click(screen.getByRole("button", { name: "수정" }));

      expect(onEdit).toHaveBeenCalledTimes(1);
    });

    it("프로필 없을 때 '프로필 등록' 버튼 클릭 시 onEdit이 호출되어야 한다", async () => {
      const user = userEvent.setup();
      const onEdit = vi.fn();

      render(<LenderProfileSection onEdit={onEdit} />);

      await user.click(screen.getByText("프로필 등록"));

      expect(onEdit).toHaveBeenCalledTimes(1);
    });
  });

  describe("선택적 필드", () => {
    it("businessName만 있을 때 상호명만 표시되어야 한다", () => {
      render(
        <LenderProfileSection
          lenderProfile={{ businessName: "테스트샵" }}
        />
      );

      expect(screen.getByText("테스트샵")).toBeInTheDocument();
      expect(screen.queryByText("소개")).not.toBeInTheDocument();
      expect(screen.queryByText("은행")).not.toBeInTheDocument();
    });

    it("description만 있을 때 소개만 표시되어야 한다", () => {
      render(
        <LenderProfileSection
          lenderProfile={{ description: "소개 내용" }}
        />
      );

      expect(screen.getByText("소개 내용")).toBeInTheDocument();
      expect(screen.queryByText("상호명")).not.toBeInTheDocument();
    });
  });
});
