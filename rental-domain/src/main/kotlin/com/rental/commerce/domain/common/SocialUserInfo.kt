package com.rental.commerce.domain.common

import com.rental.commerce.domain.user.SocialProvider

data class SocialUserInfo(
    val socialId: String,
    val email: String,
    val name: String,
    val provider: SocialProvider,
)
