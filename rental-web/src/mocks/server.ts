import { setupServer } from "msw/node";
import { authHandlers } from "./handlers/auth";
import { productHandlers } from "./handlers/product";
import { mypageHandlers } from "./handlers/mypage";
import { notificationHandlers } from "./handlers/notification";
import { rentalHandlers } from "./handlers/rental";

// ========================================
// MSW 서버 설정 (테스트용)
// ========================================

export const server = setupServer(
  ...authHandlers,
  ...productHandlers,
  ...mypageHandlers,
  ...notificationHandlers,
  ...rentalHandlers
);
