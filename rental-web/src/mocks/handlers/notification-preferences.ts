import { http, HttpResponse, delay } from "msw";
import type {
  NotificationPreferenceResult,
  UpdateNotificationPreferenceRequest,
} from "@/lib/api/types";

// ========================================
// 알림 설정 MSW 핸들러 (BE-430/431)
// ========================================

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 기본값: chat/rental/settlement=true, marketing=false
const DEFAULT_PREFERENCES: NotificationPreferenceResult = {
  userId: 1,
  chatEnabled: true,
  rentalEnabled: true,
  settlementEnabled: true,
  marketingEnabled: false,
};

let currentPreferences: NotificationPreferenceResult = {
  ...DEFAULT_PREFERENCES,
};

export function resetNotificationPreferencesHandlerState(): void {
  currentPreferences = { ...DEFAULT_PREFERENCES };
}

export const notificationPreferencesHandlers = [
  // GET /api/v1/me/notification-preferences
  http.get(
    `${BASE_URL}/api/v1/me/notification-preferences`,
    async () => {
      await delay(100);
      return HttpResponse.json({
        success: true,
        data: currentPreferences,
        timestamp: new Date().toISOString(),
      });
    }
  ),

  // PATCH /api/v1/me/notification-preferences
  http.patch(
    `${BASE_URL}/api/v1/me/notification-preferences`,
    async ({ request }) => {
      await delay(150);
      const body = (await request.json()) as UpdateNotificationPreferenceRequest;

      currentPreferences = {
        ...currentPreferences,
        ...(body.chatEnabled !== undefined && { chatEnabled: body.chatEnabled }),
        ...(body.rentalEnabled !== undefined && {
          rentalEnabled: body.rentalEnabled,
        }),
        ...(body.settlementEnabled !== undefined && {
          settlementEnabled: body.settlementEnabled,
        }),
        ...(body.marketingEnabled !== undefined && {
          marketingEnabled: body.marketingEnabled,
        }),
      };

      return HttpResponse.json({
        success: true,
        data: currentPreferences,
        timestamp: new Date().toISOString(),
      });
    }
  ),
];
