package com.rental.commerce.application.user

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenFamily: String,
    val userId: Long,
)
