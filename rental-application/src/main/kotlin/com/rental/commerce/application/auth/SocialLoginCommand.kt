package com.rental.commerce.application.auth

import com.rental.commerce.domain.common.SocialProvider

data class SocialLoginCommand(
    val provider: SocialProvider,
    val authorizationCode: String,
)
