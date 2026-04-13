import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";

// ========================================
// Layout 컴포넌트 테스트
// ========================================

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/",
}));

// next/font/google은 jsdom 환경에서 동작하지 않으므로 mock
vi.mock("next/font/google", () => ({
  Geist: () => ({
    variable: "--font-geist-sans",
    className: "geist-sans",
  }),
  Geist_Mono: () => ({
    variable: "--font-geist-mono",
    className: "geist-mono",
  }),
}));

// sonner는 toast 라이브러리, Toaster만 mock
vi.mock("@/components/ui/sonner", () => ({
  Toaster: () => null,
}));

describe("LenderLayout", () => {
  it("children을 렌더링해야 한다", async () => {
    const LenderLayout = (await import("@/app/(lender)/layout")).default;
    render(
      <LenderLayout>
        <div>lender 자식</div>
      </LenderLayout>
    );
    expect(screen.getByText("lender 자식")).toBeInTheDocument();
  });
});

describe("RenterLayout", () => {
  it("children을 렌더링해야 한다", async () => {
    const RenterLayout = (await import("@/app/(renter)/layout")).default;
    render(
      <RenterLayout>
        <div>renter 자식</div>
      </RenterLayout>
    );
    expect(screen.getByText("renter 자식")).toBeInTheDocument();
  });
});

describe("RootLayout", () => {
  it("children을 렌더링해야 한다", async () => {
    const RootLayout = (await import("@/app/layout")).default;
    render(
      <RootLayout>
        <div>루트 자식</div>
      </RootLayout>
    );
    expect(screen.getByText("루트 자식")).toBeInTheDocument();
  });
});
