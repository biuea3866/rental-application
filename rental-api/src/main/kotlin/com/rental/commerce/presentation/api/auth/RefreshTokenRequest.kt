package com.rental.commerce.presentation.api.auth

import com.rental.commerce.application.auth.RefreshTokenCommand
import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "리프레시 토큰은 필수입니다")
    val refreshToken: String,

    @field:NotBlank(message = "토큰 패밀리는 필수입니다")
    val tokenFamily: String,
) {
    fun toCommand(): RefreshTokenCommand = RefreshTokenCommand(
        refreshToken = refreshToken,
        tokenFamily = tokenFamily,
    )
}
