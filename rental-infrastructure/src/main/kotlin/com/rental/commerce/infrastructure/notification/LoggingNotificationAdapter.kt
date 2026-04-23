package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.notification.NotificationChannel
import com.rental.commerce.domain.notification.NotificationPayload
import com.rental.commerce.domain.notification.NotificationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * NotificationPort 기본 어댑터 — 로그만 기록.
 *
 * 실제 발송(이메일/푸시/SMS)은 알림 서버로 위임 예정 (별도 티켓).
 * 이 어댑터는 NotificationDispatcher → port 경계를 먼저 확정하기 위한 스텁.
 */
@Component
class LoggingNotificationAdapter : NotificationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun dispatch(userId: Long, channel: NotificationChannel, payload: NotificationPayload) {
        log.info(
            "[NotificationPort] dispatch userId={} channel={} templateId={} variables={}",
            userId, channel, payload.templateId, payload.variables,
        )
    }
}
