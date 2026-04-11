package com.rental.commerce.infrastructure.auth

import java.time.ZonedDateTime

data class JwtClaims(
    val userId: Long,
    val role: String,
    val issuedAt: ZonedDateTime,
    val expiresAt: ZonedDateTime,
)
