package com.rental.commerce.application.auth

data class RefreshTokenCommand(
    val refreshToken: String,
    val tokenFamily: String,
)
