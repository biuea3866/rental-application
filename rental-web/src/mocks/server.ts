import { setupServer } from "msw/node";
import { authHandlers } from "./handlers/auth";
import { productHandlers } from "./handlers/product";

// ========================================
// MSW 서버 설정 (테스트용)
// ========================================

export const server = setupServer(...authHandlers, ...productHandlers);
