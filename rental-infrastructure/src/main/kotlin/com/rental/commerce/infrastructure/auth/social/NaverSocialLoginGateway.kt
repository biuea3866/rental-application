package com.rental.commerce.infrastructure.auth.social

import com.fasterxml.jackson.annotation.JsonProperty
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.SocialUserInfo
import com.rental.commerce.domain.common.SocialProvider
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

class NaverSocialLoginGateway(
    private val restClient: RestClient,
    private val properties: SocialLoginProperties.NaverProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAccessToken(authorizationCode: String): String {
        logger.info("네이버 액세스 토큰 요청: authorizationCode={}", authorizationCode.take(6))

        val response = try {
            restClient.post()
                .uri(properties.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(buildTokenRequestBody(authorizationCode))
                .retrieve()
                .body(NaverTokenResponse::class.java)
        } catch (exception: Exception) {
            logger.error("네이버 액세스 토큰 발급 실패", exception)
            throw BusinessException(ErrorCode.EXTERNAL_API_ERROR, "네이버 액세스 토큰 발급 실패")
        }

        return requireNotNull(response?.accessToken) {
            "네이버 액세스 토큰 응답이 비어있습니다"
        }
    }

    fun getUserInfo(accessToken: String): SocialUserInfo {
        logger.info("네이버 사용자 정보 요청")

        val response = try {
            restClient.get()
                .uri(properties.userInfoUri)
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(NaverUserInfoResponse::class.java)
        } catch (exception: Exception) {
            logger.error("네이버 사용자 정보 조회 실패", exception)
            throw BusinessException(ErrorCode.EXTERNAL_API_ERROR, "네이버 사용자 정보 조회 실패")
        }

        val userResponse = requireNotNull(response?.response) { "네이버 사용자 정보 응답이 비어있습니다" }

        return SocialUserInfo(
            socialId = userResponse.id,
            email = userResponse.email ?: "",
            name = userResponse.name ?: "",
            provider = SocialProvider.NAVER,
        )
    }

    private fun buildTokenRequestBody(authorizationCode: String): String {
        return "grant_type=authorization_code" +
            "&client_id=${properties.clientId}" +
            "&client_secret=${properties.clientSecret}" +
            "&redirect_uri=${properties.redirectUri}" +
            "&code=$authorizationCode"
    }
}

data class NaverTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String? = null,

    @JsonProperty("token_type")
    val tokenType: String? = null,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @JsonProperty("expires_in")
    val expiresIn: Long? = null,
)

data class NaverUserInfoResponse(
    val resultcode: String? = null,
    val message: String? = null,
    val response: NaverUserDetail? = null,
) {
    data class NaverUserDetail(
        val id: String = "",
        val email: String? = null,
        val name: String? = null,
    )
}
