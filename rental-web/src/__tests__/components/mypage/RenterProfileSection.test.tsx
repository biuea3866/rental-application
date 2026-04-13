import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { RenterProfileSection } from "@/components/mypage/RenterProfileSection";
import type { RenterProfile } from "@/lib/api/types";

// ========================================
// RenterProfileSection 테스트
// ========================================

const fullRenterProfile: RenterProfile = {
  deliveryAddress: "서울 강남구 테헤란로 123",
  preferredCategories: ["ELECTRONICS", "SPORTS", "FASHION"],
};

describe("RenterProfileSection", () => {
  describe("프로필이 있을 때", () => {
    it("'대여자 프로필' 제목이 렌더링되어야 한다", () => {
      render(<RenterProfileSection renterProfile={fullRenterProfile} />);

      expect(screen.getByText("대여자 프로필")).toBeInTheDocument();
    });

    it("배송 주소가 렌더링되어야 한다", () => {
      render(<RenterProfileSection renterProfile={fullRenterProfile} />);

      expect(
        screen.getByText("서울 강남구 테헤란로 123")
      ).toBeInTheDocument();
    });

    it("선호 카테고리가 렌더링되어야 한다", () => {
      render(<RenterProfileSection renterProfile={fullRenterProfile} />);

      expect(screen.getByText("전자기기")).toBeInTheDocument();
      expect(screen.getByText("스포츠")).toBeInTheDocument();
      expect(screen.getByText("패션")).toBeInTheDocument();
    });

    it("'선호 카테고리' 레이블이 표시되어야 한다", () => {
      render(<RenterProfileSection renterProfile={fullRenterProfile} />);

      expect(screen.getByText("선호 카테고리")).toBeInTheDocument();
    });

    it("'배송 주소' 레이블이 표시되어야 한다", () => {
      render(<RenterProfileSection renterProfile={fullRenterProfile} />);

      expect(screen.getByText("배송 주소")).toBeInTheDocument();
    });
  });

  describe("프로필이 없을 때", () => {
    it("'대여자 프로필이 없습니다' 메시지가 표시되어야 한다", () => {
      render(<RenterProfileSection />);

      expect(
        screen.getByText("대여자 프로필이 없습니다.")
      ).toBeInTheDocument();
    });

    it("onEdit이 있으면 '프로필 등록' 버튼이 표시되어야 한다", () => {
      render(<RenterProfileSection onEdit={vi.fn()} />);

      expect(screen.getByText("프로필 등록")).toBeInTheDocument();
    });

    it("onEdit이 없으면 '프로필 등록' 버튼이 표시되지 않아야 한다", () => {
      render(<RenterProfileSection />);

      expect(screen.queryByText("프로필 등록")).not.toBeInTheDocument();
    });
  });

  describe("onEdit prop", () => {
    it("onEdit이 있으면 헤더에 '수정' 버튼이 표시되어야 한다", () => {
      render(
        <RenterProfileSection
          renterProfile={fullRenterProfile}
          onEdit={vi.fn()}
        />
      );

      expect(screen.getByRole("button", { name: "수정" })).toBeInTheDocument();
    });

    it("onEdit이 없으면 헤더에 '수정' 버튼이 표시되지 않아야 한다", () => {
      render(<RenterProfileSection renterProfile={fullRenterProfile} />);

      expect(screen.queryByRole("button", { name: "수정" })).not.toBeInTheDocument();
    });

    it("'수정' 버튼 클릭 시 onEdit이 호출되어야 한다", async () => {
      const user = userEvent.setup();
      const onEdit = vi.fn();

      render(
        <RenterProfileSection
          renterProfile={fullRenterProfile}
          onEdit={onEdit}
        />
      );

      await user.click(screen.getByRole("button", { name: "수정" }));

      expect(onEdit).toHaveBeenCalledTimes(1);
    });

    it("프로필 없을 때 '프로필 등록' 버튼 클릭 시 onEdit이 호출되어야 한다", async () => {
      const user = userEvent.setup();
      const onEdit = vi.fn();

      render(<RenterProfileSection onEdit={onEdit} />);

      await user.click(screen.getByText("프로필 등록"));

      expect(onEdit).toHaveBeenCalledTimes(1);
    });
  });

  describe("선호 카테고리 한글 변환", () => {
    it("모든 카테고리가 한글로 변환되어야 한다", () => {
      render(
        <RenterProfileSection
          renterProfile={{
            preferredCategories: [
              "ELECTRONICS",
              "FURNITURE",
              "SPORTS",
              "FASHION",
              "BOOKS",
              "TOOLS",
              "VEHICLES",
              "OTHERS",
            ],
          }}
        />
      );

      expect(screen.getByText("전자기기")).toBeInTheDocument();
      expect(screen.getByText("가구")).toBeInTheDocument();
      expect(screen.getByText("스포츠")).toBeInTheDocument();
      expect(screen.getByText("패션")).toBeInTheDocument();
      expect(screen.getByText("도서")).toBeInTheDocument();
      expect(screen.getByText("공구")).toBeInTheDocument();
      expect(screen.getByText("차량")).toBeInTheDocument();
      expect(screen.getByText("기타")).toBeInTheDocument();
    });

    it("빈 선호 카테고리는 표시되지 않아야 한다", () => {
      render(
        <RenterProfileSection
          renterProfile={{
            preferredCategories: [],
            deliveryAddress: "서울",
          }}
        />
      );

      expect(screen.queryByText("선호 카테고리")).not.toBeInTheDocument();
    });
  });
});
