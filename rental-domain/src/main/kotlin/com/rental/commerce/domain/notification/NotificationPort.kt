package com.rental.commerce.domain.notification

/**
 * 실제 알림 발송 Port (이메일/푸시/SMS 어댑터가 구현).
 *
 * !! 주의 !!
 * 이 Port 는 직접 주입해 사용하지 마세요.
 * 반드시 NotificationDispatcher 경유 (사용자별 NotificationPreference 체크).
 *
 * ArchUnit 규칙으로 도메인 외부의 직접 주입을 차단 (BE-431).
 */
interface NotificationPort {
    fun dispatch(userId: Long, channel: NotificationChannel, payload: NotificationPayload)
}

data class NotificationPayload(
    val templateId: String,
    val variables: Map<String, Any?> = emptyMap(),
)
