package com.rental.commerce.domain.notification

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * NotificationDispatcher (BE-431, PRD-004 §4.5).
 *
 * 모든 알림 발송 경로는 이 Dispatcher 경유가 강제된다.
 * preference off 시 발송 스킵.
 *
 * 신규 가입자의 기본 preference 는 UserCreated 이벤트에서 생성 (V23 백필은 기존 가입자용).
 */
@Component
class NotificationDispatcher(
    private val preferenceRepository: NotificationPreferenceRepository,
    private val notificationPort: NotificationPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send(userId: Long, channel: NotificationChannel, payload: NotificationPayload) {
        val preference = preferenceRepository.findByUserId(userId)
            ?: NotificationPreference.createDefault(userId) // 누락 시 기본값 허용(안전 default)

        if (!preference.isEnabled(channel)) {
            log.debug(
                "[NotificationDispatcher] 채널 비활성 — 스킵 userId={} channel={}",
                userId, channel,
            )
            return
        }
        notificationPort.dispatch(userId, channel, payload)
    }
}
