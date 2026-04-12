package com.rental.commerce.infrastructure.auth.social

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(SocialLoginProperties::class)
class SocialLoginConfig(
    private val socialLoginProperties: SocialLoginProperties,
) {

    @Bean
    fun socialRestClient(): RestClient {
        return RestClient.builder().build()
    }

    @Bean
    fun kakaoSocialLoginGateway(socialRestClient: RestClient): KakaoSocialLoginGateway {
        return KakaoSocialLoginGateway(
            restClient = socialRestClient,
            properties = socialLoginProperties.kakao,
        )
    }

    @Bean
    fun naverSocialLoginGateway(socialRestClient: RestClient): NaverSocialLoginGateway {
        return NaverSocialLoginGateway(
            restClient = socialRestClient,
            properties = socialLoginProperties.naver,
        )
    }
}
