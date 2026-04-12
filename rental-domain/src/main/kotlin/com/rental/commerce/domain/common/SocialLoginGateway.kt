package com.rental.commerce.domain.common

import com.rental.commerce.domain.user.SocialProvider

interface SocialLoginGateway {

    fun getAccessToken(provider: SocialProvider, authorizationCode: String): String

    fun getUserInfo(provider: SocialProvider, accessToken: String): SocialUserInfo
}
