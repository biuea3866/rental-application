import { setupWorker } from "msw/browser";
import { authHandlers } from "./handlers/auth";
import { productHandlers } from "./handlers/product";
import { mypageHandlers } from "./handlers/mypage";
import { notificationHandlers } from "./handlers/notification";
import { rentalHandlers } from "./handlers/rental";
import { chatHandlers } from "./handlers/chat";
import { adminHandlers } from "./handlers/admin";

// ========================================
// MSW 브라우저 워커 설정
// ========================================

export const worker = setupWorker(
  ...authHandlers,
  ...productHandlers,
  ...mypageHandlers,
  ...notificationHandlers,
  ...rentalHandlers,
  ...chatHandlers,
  ...adminHandlers
);
