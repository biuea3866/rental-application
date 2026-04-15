package com.rental.commerce.presentation.api.auth

import com.fasterxml.jackson.annotation.JsonAlias
import com.rental.commerce.application.auth.SocialLoginCommand
import com.rental.commerce.domain.user.SocialProvider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SocialLoginRequest(
    @field:NotNull(message = "소셜 프로바이더는 필수입니다")
    val provider: SocialProvider?,

    // CRT-002: FE가 "code"로 전송하므로 @JsonAlias로 양쪽 모두 수용
    @field:NotBlank(message = "인가 코드는 필수입니다")
    @JsonAlias("code")
    val authorizationCode: String,
) {
    fun toCommand(): SocialLoginCommand = SocialLoginCommand(
        provider = requireNotNull(provider) { "소셜 프로바이더는 필수입니다" },
        authorizationCode = authorizationCode,
    )
}
