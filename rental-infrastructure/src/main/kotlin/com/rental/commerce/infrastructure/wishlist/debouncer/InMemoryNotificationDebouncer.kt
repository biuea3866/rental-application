package com.rental.commerce.infrastructure.wishlist.debouncer

import com.rental.commerce.domain.wishlist.port.NotificationDebouncer
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 로컬/테스트용 In-Memory debouncer.
 *
 * 운영 환경에서는 Redis 기반 adapter(RedisNotificationDebouncer) 사용 예정.
 * 멀티 노드 환경에서는 효과 없음 — Redis 전환 필수.
 */
@Component
@Profile("local", "dev", "test")
class InMemoryNotificationDebouncer : NotificationDebouncer {

    private val store: ConcurrentHashMap<String, Instant> = ConcurrentHashMap()

    override fun tryAcquire(userId: Long, productId: Long, ttl: Duration): Boolean {
        val now = Instant.now()
        val key = "$userId:$productId"
        val prev = store[key]
        if (prev != null && prev.isAfter(now)) return false
        store[key] = now.plus(ttl)
        return true
    }
}
