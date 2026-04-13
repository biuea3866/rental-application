import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ProfileEditForm } from "@/components/mypage/ProfileEditForm";
import type { MyPageInfo } from "@/lib/api/types";

// ========================================
// ProfileEditForm 테스트
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/mypage/edit",
}));

const mockMyPageInfo: MyPageInfo = {
  id: "user-lender-001",
  email: "lender@rental.com",
  name: "김대여",
  phone: "010-1234-5678",
  role: "LENDER",
  createdAt: "2026-01-15T09:00:00Z",
};

describe("ProfileEditForm", () => {
  it("기본 정보 수정 제목이 렌더링되어야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);
    expect(screen.getByText("기본 정보 수정")).toBeInTheDocument();
  });

  it("이름 필드가 초기값으로 채워져야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);
    expect(screen.getByDisplayValue("김대여")).toBeInTheDocument();
  });

  it("이메일 필드가 초기값으로 채워져야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);
    expect(screen.getByDisplayValue("lender@rental.com")).toBeInTheDocument();
  });

  it("전화번호 필드가 초기값으로 채워져야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);
    expect(screen.getByDisplayValue("010-1234-5678")).toBeInTheDocument();
  });

  it("이메일 필드가 비활성화되어야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);
    const emailInput = screen.getByDisplayValue("lender@rental.com");
    expect(emailInput).toBeDisabled();
  });

  it("'이메일은 변경할 수 없습니다' 안내 메시지가 있어야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);
    expect(screen.getByText("이메일은 변경할 수 없습니다.")).toBeInTheDocument();
  });

  it("저장 버튼이 렌더링되어야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);
    expect(screen.getByRole("button", { name: "저장" })).toBeInTheDocument();
  });

  it("isSubmitting이 true일 때 '저장 중...' 텍스트와 버튼 비활성화", () => {
    render(
      <ProfileEditForm
        myPageInfo={mockMyPageInfo}
        onSubmit={vi.fn()}
        isSubmitting={true}
      />
    );

    const submitButton = screen.getByRole("button", { name: "저장 중..." });
    expect(submitButton).toBeDisabled();
  });

  it("유효한 데이터로 제출 시 onSubmit이 호출되어야 한다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);

    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={onSubmit} />);

    // 이름 변경
    const nameInput = screen.getByDisplayValue("김대여");
    await user.clear(nameInput);
    await user.type(nameInput, "홍길동");

    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        name: "홍길동",
        phone: "010-1234-5678",
        profileImageUrl: undefined,
      });
    });
  });

  it("이름이 2자 미만이면 유효성 에러가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={onSubmit} />);

    const nameInput = screen.getByDisplayValue("김대여");
    await user.clear(nameInput);
    await user.type(nameInput, "김");

    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => {
      expect(screen.getByText("이름은 2자 이상이어야 합니다.")).toBeInTheDocument();
    });

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("전화번호 형식이 잘못되면 유효성 에러가 표시되어야 한다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={onSubmit} />);

    const phoneInput = screen.getByDisplayValue("010-1234-5678");
    await user.clear(phoneInput);
    await user.type(phoneInput, "01012345678"); // 잘못된 형식

    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => {
      expect(
        screen.getByText(/전화번호 형식이 올바르지 않습니다/)
      ).toBeInTheDocument();
    });

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("프로필 이미지 URL 입력 필드가 렌더링되어야 한다", () => {
    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={vi.fn()} />);

    // URL 입력 필드가 있어야 한다
    expect(
      screen.getByPlaceholderText("https://example.com/image.jpg")
    ).toBeInTheDocument();
  });

  it("프로필 이미지 URL이 비어 있으면 undefined로 전달되어야 한다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);

    render(<ProfileEditForm myPageInfo={mockMyPageInfo} onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({ profileImageUrl: undefined })
      );
    });
  });
});
