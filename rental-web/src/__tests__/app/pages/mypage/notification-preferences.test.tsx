import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithQuery } from "@/__tests__/test-utils";
import { server } from "@/mocks/server";
import { http, HttpResponse } from "msw";

// ========================================
// 알림 설정 페이지 테스트 (FE-454)
// ========================================

const BASE_URL = "http://localhost:8080";

describe("NotificationPreferencesPage", () => {
  it("기본값을 불러와 토글 상태를 올바르게 렌더링한다", async () => {
    const { NotificationPreferencesPage } = await import(
      "@/app/(auth)/mypage/notification-preferences/page"
    );

    renderWithQuery(<NotificationPreferencesPage />);

    // 로딩 중
    expect(screen.getByText(/불러오는 중/)).toBeInTheDocument();

    // 데이터 로드 후 토글 확인
    await waitFor(() => {
      expect(screen.getByRole("switch", { name: /채팅 알림/ })).toBeInTheDocument();
    });

    const chatToggle = screen.getByRole("switch", { name: /채팅 알림/ });
    const rentalToggle = screen.getByRole("switch", { name: /대여 알림/ });
    const settlementToggle = screen.getByRole("switch", { name: /정산 알림/ });
    const marketingToggle = screen.getByRole("switch", { name: /마케팅 알림/ });

    // 기본값: chat/rental/settlement=true, marketing=false
    expect(chatToggle).toHaveAttribute("aria-checked", "true");
    expect(rentalToggle).toHaveAttribute("aria-checked", "true");
    expect(settlementToggle).toHaveAttribute("aria-checked", "true");
    expect(marketingToggle).toHaveAttribute("aria-checked", "false");
  });

  it("마케팅 동의 문구(법적 고지)가 렌더링된다", async () => {
    const { NotificationPreferencesPage } = await import(
      "@/app/(auth)/mypage/notification-preferences/page"
    );

    renderWithQuery(<NotificationPreferencesPage />);

    await waitFor(() => {
      expect(screen.getByRole("switch", { name: /마케팅 알림/ })).toBeInTheDocument();
    });

    // 법적 동의 문구 섹션 존재 확인
    const legalSection = screen.getByRole("region", {
      name: /마케팅 동의 안내/,
    });
    expect(legalSection).toBeInTheDocument();
    expect(legalSection).toHaveTextContent(/개인정보/);
    expect(legalSection).toHaveTextContent(/수신 동의/);
  });

  it("토글 클릭 후 저장 성공 시 낙관적 업데이트되고 토스트가 표시된다", async () => {
    const user = userEvent.setup();
    const { NotificationPreferencesPage } = await import(
      "@/app/(auth)/mypage/notification-preferences/page"
    );

    const onSave = vi.fn();

    renderWithQuery(<NotificationPreferencesPage onSaveSuccess={onSave} />);

    await waitFor(() => {
      expect(screen.getByRole("switch", { name: /마케팅 알림/ })).toBeInTheDocument();
    });

    const marketingToggle = screen.getByRole("switch", { name: /마케팅 알림/ });
    expect(marketingToggle).toHaveAttribute("aria-checked", "false");

    await user.click(marketingToggle);

    // 낙관적 업데이트: 클릭 직후 반영
    expect(marketingToggle).toHaveAttribute("aria-checked", "true");

    // 저장 성공 후 콜백 호출
    await waitFor(() => {
      expect(onSave).toHaveBeenCalled();
    });
  });

  it("저장 실패 시 낙관적 업데이트를 롤백한다", async () => {
    const user = userEvent.setup();

    server.use(
      http.patch(`${BASE_URL}/api/v1/me/notification-preferences`, () => {
        return HttpResponse.json(
          { code: "INTERNAL_ERROR", message: "서버 오류" },
          { status: 500 }
        );
      })
    );

    const onError = vi.fn();
    const { NotificationPreferencesPage } = await import(
      "@/app/(auth)/mypage/notification-preferences/page"
    );

    renderWithQuery(<NotificationPreferencesPage onSaveError={onError} />);

    await waitFor(() => {
      expect(screen.getByRole("switch", { name: /마케팅 알림/ })).toBeInTheDocument();
    });

    const marketingToggle = screen.getByRole("switch", { name: /마케팅 알림/ });
    // 초기: OFF
    expect(marketingToggle).toHaveAttribute("aria-checked", "false");

    // 클릭 후 낙관적 업데이트 + 서버 응답 대기
    await act(async () => {
      await user.click(marketingToggle);
    });

    // 롤백: OFF 복구 (서버 실패로 인해)
    await waitFor(() => {
      expect(marketingToggle).toHaveAttribute("aria-checked", "false");
    });

    // 에러 콜백 호출
    expect(onError).toHaveBeenCalled();
  });
});

describe("ChannelToggle", () => {
  it("role=switch + aria-checked 속성을 갖는다", async () => {
    const { ChannelToggle } = await import(
      "@/components/mypage/ChannelToggle"
    );

    const handleChange = vi.fn();

    render(
      <ChannelToggle
        label="채팅 알림"
        checked={true}
        onChange={handleChange}
      />
    );

    const toggle = screen.getByRole("switch", { name: /채팅 알림/ });
    expect(toggle).toHaveAttribute("aria-checked", "true");
  });

  it("클릭 시 onChange 콜백을 호출한다", async () => {
    const user = userEvent.setup();
    const { ChannelToggle } = await import(
      "@/components/mypage/ChannelToggle"
    );

    const handleChange = vi.fn();

    render(
      <ChannelToggle
        label="대여 알림"
        checked={false}
        onChange={handleChange}
      />
    );

    const toggle = screen.getByRole("switch", { name: /대여 알림/ });
    await user.click(toggle);

    expect(handleChange).toHaveBeenCalledWith(true);
  });

  it("disabled 상태일 때 클릭이 무시된다", async () => {
    const user = userEvent.setup();
    const { ChannelToggle } = await import(
      "@/components/mypage/ChannelToggle"
    );

    const handleChange = vi.fn();

    render(
      <ChannelToggle
        label="정산 알림"
        checked={true}
        onChange={handleChange}
        disabled
      />
    );

    const toggle = screen.getByRole("switch", { name: /정산 알림/ });
    expect(toggle).toBeDisabled();
    await user.click(toggle);

    expect(handleChange).not.toHaveBeenCalled();
  });
});
