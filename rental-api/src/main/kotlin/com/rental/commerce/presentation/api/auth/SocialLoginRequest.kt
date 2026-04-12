package com.rental.commerce.presentation.api.auth

import com.rental.commerce.application.auth.SocialLoginCommand
import com.rental.commerce.domain.user.SocialProvider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SocialLoginRequest(
    @field:NotNull(message = "소셜 프로바이더는 필수입니다")
    val provider: SocialProvider?,

    @field:NotBlank(message = "인가 코드는 필수입니다")
    val authorizationCode: String,
) {
    fun toCommand(): SocialLoginCommand = SocialLoginCommand(
        provider = requireNotNull(provider) { "소셜 프로바이더는 필수입니다" },
        authorizationCode = authorizationCode,
    )
}
