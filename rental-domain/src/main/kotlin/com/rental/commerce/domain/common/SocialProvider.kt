package com.rental.commerce.domain.common

enum class SocialProvider {
    KAKAO,
    NAVER,
    GOOGLE,
    APPLE;

    fun providesPhoneNumber(): Boolean = this == KAKAO || this == NAVER
}
