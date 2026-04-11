package com.rental.commerce.infrastructure.auth

import com.rental.commerce.domain.common.PhoneVerificationStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisPhoneVerificationStore(
    private val redisTemplate: StringRedisTemplate,
) : PhoneVerificationStore {

    companion object {
        private const val KEY_PREFIX = "phone:verify:"
    }

    override fun save(phone: String, code: String, ttlSeconds: Long) {
        val key = buildKey(phone)
        redisTemplate.opsForValue().set(key, code, ttlSeconds, TimeUnit.SECONDS)
    }

    override fun findByPhone(phone: String): String? {
        val key = buildKey(phone)
        return redisTemplate.opsForValue().get(key)
    }

    override fun delete(phone: String) {
        val key = buildKey(phone)
        redisTemplate.delete(key)
    }

    private fun buildKey(phone: String): String = "$KEY_PREFIX$phone"
}
