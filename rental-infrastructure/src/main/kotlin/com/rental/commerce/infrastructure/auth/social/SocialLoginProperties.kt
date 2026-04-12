package com.rental.commerce.infrastructure.auth.social

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "social")
data class SocialLoginProperties(
    val kakao: KakaoProperties,
    val naver: NaverProperties,
) {
    data class KakaoProperties(
        val clientId: String,
        val clientSecret: String,
        val redirectUri: String,
        val tokenUri: String = "https://kauth.kakao.com/oauth/token",
        val userInfoUri: String = "https://kapi.kakao.com/v2/user/me",
    )

    data class NaverProperties(
        val clientId: String,
        val clientSecret: String,
        val redirectUri: String,
        val tokenUri: String = "https://nid.naver.com/oauth2.0/token",
        val userInfoUri: String = "https://openapi.naver.com/v1/nid/me",
    )
}
