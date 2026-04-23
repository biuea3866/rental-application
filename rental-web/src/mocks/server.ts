import { setupServer } from "msw/node";
import { authHandlers } from "./handlers/auth";
import { productHandlers } from "./handlers/product";
import { mypageHandlers } from "./handlers/mypage";
import { notificationHandlers } from "./handlers/notification";
import { notificationPreferencesHandlers } from "./handlers/notification-preferences";
import { rentalHandlers } from "./handlers/rental";
import { reviewHandlers } from "./handlers/review";
import { settlementHandlers } from "./handlers/settlement";
import { chatHandlers } from "./handlers/chat";
import { adminHandlers } from "./handlers/admin";
import { wishlistHandlers } from "./handlers/wishlist";
import { disputeHandlers } from "./handlers/dispute";

// ========================================
// MSW 서버 설정 (테스트용)
// ========================================

export const server = setupServer(
  ...authHandlers,
  ...productHandlers,
  ...mypageHandlers,
  ...notificationHandlers,
  ...notificationPreferencesHandlers,
  ...rentalHandlers,
  ...reviewHandlers,
  ...settlementHandlers,
  ...chatHandlers,
  ...adminHandlers,
  ...wishlistHandlers,
  ...disputeHandlers
);
