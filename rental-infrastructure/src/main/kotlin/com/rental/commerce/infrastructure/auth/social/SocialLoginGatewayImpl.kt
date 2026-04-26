package com.rental.commerce.infrastructure.auth.social

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.SocialLoginGateway
import com.rental.commerce.domain.common.SocialUserInfo
import com.rental.commerce.domain.common.SocialProvider
import org.springframework.stereotype.Component

@Component
class SocialLoginGatewayImpl(
    private val kakaoGateway: KakaoSocialLoginGateway,
    private val naverGateway: NaverSocialLoginGateway,
) : SocialLoginGateway {

    override fun getAccessToken(provider: SocialProvider, authorizationCode: String): String {
        return when (provider) {
            SocialProvider.KAKAO -> kakaoGateway.getAccessToken(authorizationCode)
            SocialProvider.NAVER -> naverGateway.getAccessToken(authorizationCode)
            else -> throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "지원하지 않는 소셜 프로바이더입니다: $provider",
            )
        }
    }

    override fun getUserInfo(provider: SocialProvider, accessToken: String): SocialUserInfo {
        return when (provider) {
            SocialProvider.KAKAO -> kakaoGateway.getUserInfo(accessToken)
            SocialProvider.NAVER -> naverGateway.getUserInfo(accessToken)
            else -> throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "지원하지 않는 소셜 프로바이더입니다: $provider",
            )
        }
    }
}
