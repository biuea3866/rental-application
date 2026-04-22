package com.rental.commerce.domain.notification

/**
 * 알림 채널 (PRD-004 §4.5).
 *
 * 기본값:
 *  CHAT, RENTAL, SETTLEMENT = ON (필수 거래 흐름 알림)
 *  MARKETING = OFF (opt-in)
 */
enum class NotificationChannel {
    CHAT,
    RENTAL,
    SETTLEMENT,
    MARKETING,
}
