import { setupWorker } from "msw/browser";
import { authHandlers } from "./handlers/auth";
import { productHandlers } from "./handlers/product";

// ========================================
// MSW 브라우저 워커 설정
// ========================================

export const worker = setupWorker(...authHandlers, ...productHandlers);
