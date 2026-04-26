package com.rental.commerce.infrastructure.wishlist.debouncer

import com.rental.commerce.domain.wishlist.port.NotificationDebouncer
import java.time.Duration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * Redis SETNX 기반 NotificationDebouncer (RC-DEVOPS-446).
 *
 * 다중 노드 환경에서 동일 사용자/상품에 대한 중복 알림을 방지한다.
 * SETNX(setIfAbsent) + TTL 패턴으로 분산 debounce를 구현한다.
 *
 * 키 포맷: debounce:notification:{userId}:{productId}
 * 값: "1" (단순 존재 여부 확인)
 *
 * @Profile("prod") — 운영 환경에서만 활성화.
 * 로컬/개발/테스트는 InMemoryNotificationDebouncer 사용.
 */
@Component
@Profile("prod")
class RedisNotificationDebouncer(
    private val redisTemplate: StringRedisTemplate,
) : NotificationDebouncer {

    companion object {
        private const val KEY_PREFIX = "debounce:notification"
    }

    /**
     * Redis SETNX를 사용하여 debounce 키 선점을 시도한다.
     *
     * @return true = 키 선점 성공 → 발송 허용, false = 키 이미 존재 → debounce 스킵
     */
    override fun tryAcquire(userId: Long, productId: Long, ttl: Duration): Boolean {
        val key = buildKey(userId, productId)
        return redisTemplate.opsForValue().setIfAbsent(key, "1", ttl) == true
    }

    private fun buildKey(userId: Long, productId: Long): String =
        "$KEY_PREFIX:$userId:$productId"
}
