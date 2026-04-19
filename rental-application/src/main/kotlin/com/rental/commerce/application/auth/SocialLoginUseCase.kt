package com.rental.commerce.application.auth

import com.rental.commerce.application.user.AuthTokenResponse
import com.rental.commerce.domain.auth.AuthDomainService
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialLoginUseCase(
    private val authDomainService: AuthDomainService,
    private val userDomainService: UserDomainService,
    private val tokenProvider: TokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional
    fun execute(command: SocialLoginCommand): AuthTokenResponse {
        val socialUserInfo = authDomainService.authenticateWithSocialProvider(
            command.provider,
            command.authorizationCode,
        )

        val user = userDomainService.findOrCreateSocialUser(socialUserInfo)
        val userId = requireNotNull(user.id) { "저장된 사용자의 ID가 없습니다" }

        val accessToken = tokenProvider.createAccessToken(userId, user.role.name)
        val refreshTokenResult = refreshTokenService.issueRefreshToken(userId)

        return AuthTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenResult.refreshToken,
            tokenFamily = refreshTokenResult.tokenFamily,
            userId = userId,
        )
    }
}
