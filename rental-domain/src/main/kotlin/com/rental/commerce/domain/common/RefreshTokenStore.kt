package com.rental.commerce.domain.common

data class RefreshTokenData(
    val userId: Long,
    val refreshToken: String,
    val tokenFamily: String,
)

interface RefreshTokenStore {

    fun save(tokenFamily: String, refreshToken: String, userId: Long, expiryDays: Long)

    fun findByTokenFamily(tokenFamily: String): RefreshTokenData?

    fun deleteByTokenFamily(tokenFamily: String)

    fun isTokenUsed(tokenFamily: String, refreshToken: String): Boolean

    fun markTokenAsUsed(tokenFamily: String, refreshToken: String)
}
