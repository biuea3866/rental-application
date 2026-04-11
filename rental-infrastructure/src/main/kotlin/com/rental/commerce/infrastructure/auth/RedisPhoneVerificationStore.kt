package com.rental.commerce.infrastructure.auth

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PhoneVerificationStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisPhoneVerificationStore(
    private val redisTemplate: StringRedisTemplate,
) : PhoneVerificationStore {

    companion object {
        private const val CODE_KEY_PREFIX = "phone:verify:code:"
        private const val EMAIL_KEY_PREFIX = "phone:verify:email:"
    }

    override fun save(phone: String, code: String, email: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(buildCodeKey(phone), code, ttlSeconds, TimeUnit.SECONDS)
        redisTemplate.opsForValue().set(buildEmailKey(phone), email, ttlSeconds, TimeUnit.SECONDS)
    }

    override fun verify(phone: String, code: String): String {
        val storedCode = redisTemplate.opsForValue().get(buildCodeKey(phone))
            ?: throw BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED)

        if (storedCode != code) {
            throw BusinessException(ErrorCode.INVALID_VERIFICATION_CODE)
        }

        return redisTemplate.opsForValue().get(buildEmailKey(phone))
            ?: throw BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED)
    }

    override fun delete(phone: String) {
        redisTemplate.delete(buildCodeKey(phone))
        redisTemplate.delete(buildEmailKey(phone))
    }

    private fun buildCodeKey(phone: String): String = "$CODE_KEY_PREFIX$phone"
    private fun buildEmailKey(phone: String): String = "$EMAIL_KEY_PREFIX$phone"
}
