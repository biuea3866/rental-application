import { getApiClient } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  NotificationPreferenceResult,
  UpdateNotificationPreferenceRequest,
} from "./types";

// ========================================
// 알림 설정 API (BE-430/431)
// ========================================

/** GET /api/v1/me/notification-preferences — 알림 설정 조회 */
export async function getNotificationPreferencesApi() {
  const client = getApiClient();
  return client.get<NotificationPreferenceResult>(
    ENDPOINTS.NOTIFICATION_PREFERENCES.BASE
  );
}

/** PATCH /api/v1/me/notification-preferences — 알림 설정 부분 수정 */
export async function updateNotificationPreferencesApi(
  request: UpdateNotificationPreferenceRequest
) {
  const client = getApiClient();
  return client.patch<NotificationPreferenceResult>(
    ENDPOINTS.NOTIFICATION_PREFERENCES.BASE,
    request
  );
}
