import "@testing-library/jest-dom/vitest";
import { server } from "@/mocks/server";
import { resetAuthHandlerState } from "@/mocks/handlers/auth";
import { resetMyPageHandlerState } from "@/mocks/handlers/mypage";
import { resetNotificationHandlerState } from "@/mocks/handlers/notification";
import { afterAll, afterEach, beforeAll, beforeEach, vi } from "vitest";

// ========================================
// IntersectionObserver 폴리필 (jsdom 호환)
// ========================================

class IntersectionObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
  constructor() {}
}

Object.defineProperty(globalThis, "IntersectionObserver", {
  writable: true,
  configurable: true,
  value: IntersectionObserverMock,
});

// ========================================
// localStorage 폴리필 (jsdom 호환)
// ========================================

const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem(key: string) {
      return store[key] ?? null;
    },
    setItem(key: string, value: string) {
      store[key] = String(value);
    },
    removeItem(key: string) {
      delete store[key];
    },
    clear() {
      store = {};
    },
    get length() {
      return Object.keys(store).length;
    },
    key(index: number) {
      return Object.keys(store)[index] ?? null;
    },
  };
})();

Object.defineProperty(globalThis, "localStorage", {
  value: localStorageMock,
  writable: true,
});

// ========================================
// MSW 서버 설정 (테스트 환경)
// ========================================

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
beforeEach(() => {
  resetAuthHandlerState();
  resetMyPageHandlerState();
  resetNotificationHandlerState();
});
afterEach(() => {
  server.resetHandlers();
  localStorageMock.clear();
});
afterAll(() => server.close());
