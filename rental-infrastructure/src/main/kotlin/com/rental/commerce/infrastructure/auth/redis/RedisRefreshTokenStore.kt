package com.rental.commerce.infrastructure.auth.redis

import com.rental.commerce.domain.common.RefreshTokenData
import com.rental.commerce.domain.common.RefreshTokenStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisRefreshTokenStore(
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenStore {

    companion object {
        private const val TOKEN_KEY_PREFIX = "refresh_token:family:"
        private const val USED_TOKEN_KEY_PREFIX = "refresh_token:used:"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_REFRESH_TOKEN = "refreshToken"
        private const val FIELD_TOKEN_FAMILY = "tokenFamily"
    }

    override fun save(tokenFamily: String, refreshToken: String, userId: Long, expiryDays: Long) {
        val key = buildTokenKey(tokenFamily)

        val entries = mapOf(
            FIELD_USER_ID to userId.toString(),
            FIELD_REFRESH_TOKEN to refreshToken,
            FIELD_TOKEN_FAMILY to tokenFamily,
        )

        redisTemplate.opsForHash<String, String>().putAll(key, entries)
        redisTemplate.expire(key, Duration.ofDays(expiryDays))
    }

    override fun findByTokenFamily(tokenFamily: String): RefreshTokenData? {
        val key = buildTokenKey(tokenFamily)
        val entries = redisTemplate.opsForHash<String, String>().entries(key)

        if (entries.isEmpty()) {
            return null
        }

        val userId = entries[FIELD_USER_ID]?.toLongOrNull() ?: return null
        val refreshToken = entries[FIELD_REFRESH_TOKEN] ?: return null
        val family = entries[FIELD_TOKEN_FAMILY] ?: return null

        return RefreshTokenData(
            userId = userId,
            refreshToken = refreshToken,
            tokenFamily = family,
        )
    }

    override fun deleteByTokenFamily(tokenFamily: String) {
        val tokenKey = buildTokenKey(tokenFamily)
        val usedKeyPattern = buildUsedTokenKeyPrefix(tokenFamily)

        redisTemplate.delete(tokenKey)

        val usedKeys = redisTemplate.keys("$usedKeyPattern*")
        if (usedKeys.isNotEmpty()) {
            redisTemplate.delete(usedKeys)
        }
    }

    override fun isTokenUsed(tokenFamily: String, refreshToken: String): Boolean {
        val key = buildUsedTokenKey(tokenFamily, refreshToken)
        return redisTemplate.hasKey(key)
    }

    override fun markTokenAsUsed(tokenFamily: String, refreshToken: String) {
        val key = buildUsedTokenKey(tokenFamily, refreshToken)
        redisTemplate.opsForValue().set(key, "used", Duration.ofDays(14))
    }

    private fun buildTokenKey(tokenFamily: String): String {
        return "$TOKEN_KEY_PREFIX$tokenFamily"
    }

    private fun buildUsedTokenKeyPrefix(tokenFamily: String): String {
        return "$USED_TOKEN_KEY_PREFIX$tokenFamily:"
    }

    private fun buildUsedTokenKey(tokenFamily: String, refreshToken: String): String {
        return "$USED_TOKEN_KEY_PREFIX$tokenFamily:$refreshToken"
    }
}
