package com.rental.commerce.domain.wishlist.port

import java.time.Duration

/**
 * 위시리스트 알림 debounce Port (PRD-004 §4.4, TDD-004 §7).
 *
 * - 운영: Redis SETNX(key, TTL) 기반 어댑터
 * - 테스트: In-memory concurrent map 어댑터
 *
 * @return true = 발송 허용, false = debounce(최근 알림 있음)로 스킵
 */
interface NotificationDebouncer {
    fun tryAcquire(userId: Long, productId: Long, ttl: Duration): Boolean
}
