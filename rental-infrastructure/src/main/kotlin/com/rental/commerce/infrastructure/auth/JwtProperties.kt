package com.rental.commerce.infrastructure.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val accessTokenExpiry: Duration = Duration.ofMinutes(30),
    val refreshTokenExpiry: Duration = Duration.ofDays(14),
    val issuer: String = "rental-commerce",
)
