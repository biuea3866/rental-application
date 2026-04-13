import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { SocialLoginButtons } from "@/components/auth/SocialLoginButtons";

// ========================================
// SocialLoginButtons 테스트
// ========================================

describe("SocialLoginButtons", () => {
  it("카카오 로그인 버튼이 렌더링되어야 한다", () => {
    render(<SocialLoginButtons onSocialLogin={vi.fn()} />);

    expect(
      screen.getByRole("button", { name: /카카오로 로그인/i })
    ).toBeInTheDocument();
  });

  it("네이버 로그인 버튼이 렌더링되어야 한다", () => {
    render(<SocialLoginButtons onSocialLogin={vi.fn()} />);

    expect(
      screen.getByRole("button", { name: /네이버로 로그인/i })
    ).toBeInTheDocument();
  });

  it("'소셜 로그인' 구분선이 렌더링되어야 한다", () => {
    render(<SocialLoginButtons onSocialLogin={vi.fn()} />);

    expect(screen.getByText("소셜 로그인")).toBeInTheDocument();
  });

  it("카카오 버튼 클릭 시 onSocialLogin이 'KAKAO'와 함께 호출되어야 한다", async () => {
    const user = userEvent.setup();
    const onSocialLogin = vi.fn();

    render(<SocialLoginButtons onSocialLogin={onSocialLogin} />);

    await user.click(screen.getByRole("button", { name: /카카오로 로그인/i }));

    expect(onSocialLogin).toHaveBeenCalledWith("KAKAO");
    expect(onSocialLogin).toHaveBeenCalledTimes(1);
  });

  it("네이버 버튼 클릭 시 onSocialLogin이 'NAVER'와 함께 호출되어야 한다", async () => {
    const user = userEvent.setup();
    const onSocialLogin = vi.fn();

    render(<SocialLoginButtons onSocialLogin={onSocialLogin} />);

    await user.click(screen.getByRole("button", { name: /네이버로 로그인/i }));

    expect(onSocialLogin).toHaveBeenCalledWith("NAVER");
    expect(onSocialLogin).toHaveBeenCalledTimes(1);
  });

  it("isLoading이 true일 때 버튼들이 비활성화되어야 한다", () => {
    render(<SocialLoginButtons onSocialLogin={vi.fn()} isLoading={true} />);

    const kakaoButton = screen.getByRole("button", {
      name: /카카오로 로그인/i,
    });
    const naverButton = screen.getByRole("button", {
      name: /네이버로 로그인/i,
    });

    expect(kakaoButton).toBeDisabled();
    expect(naverButton).toBeDisabled();
  });

  it("isLoading이 false일 때 버튼들이 활성화되어야 한다", () => {
    render(<SocialLoginButtons onSocialLogin={vi.fn()} isLoading={false} />);

    const kakaoButton = screen.getByRole("button", {
      name: /카카오로 로그인/i,
    });
    const naverButton = screen.getByRole("button", {
      name: /네이버로 로그인/i,
    });

    expect(kakaoButton).not.toBeDisabled();
    expect(naverButton).not.toBeDisabled();
  });

  it("isLoading이 true일 때 버튼을 클릭해도 onSocialLogin이 호출되지 않아야 한다", async () => {
    const user = userEvent.setup();
    const onSocialLogin = vi.fn();

    render(<SocialLoginButtons onSocialLogin={onSocialLogin} isLoading={true} />);

    const kakaoButton = screen.getByRole("button", {
      name: /카카오로 로그인/i,
    });

    await user.click(kakaoButton);

    expect(onSocialLogin).not.toHaveBeenCalled();
  });

  it("카카오 버튼에 aria-label이 설정되어야 한다", () => {
    render(<SocialLoginButtons onSocialLogin={vi.fn()} />);

    expect(screen.getByLabelText("카카오로 로그인")).toBeInTheDocument();
  });

  it("네이버 버튼에 aria-label이 설정되어야 한다", () => {
    render(<SocialLoginButtons onSocialLogin={vi.fn()} />);

    expect(screen.getByLabelText("네이버로 로그인")).toBeInTheDocument();
  });
});
