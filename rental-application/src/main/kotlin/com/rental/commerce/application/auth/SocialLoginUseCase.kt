package com.rental.commerce.application.auth

import com.rental.commerce.application.user.AuthTokenResponse
import com.rental.commerce.domain.common.SocialLoginGateway
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialLoginUseCase(
    private val socialLoginGateway: SocialLoginGateway,
    private val userRepository: UserRepository,
    private val tokenProvider: TokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional
    fun execute(command: SocialLoginCommand): AuthTokenResponse {
        val providerAccessToken = socialLoginGateway.getAccessToken(command.provider, command.authorizationCode)
        val socialUserInfo = socialLoginGateway.getUserInfo(command.provider, providerAccessToken)

        val user = findOrCreateUser(socialUserInfo)
        val userId = requireNotNull(user.userId) { "저장된 사용자의 ID가 없습니다" }

        val accessToken = tokenProvider.createAccessToken(userId, user.role.name)
        val refreshTokenResult = refreshTokenService.issueRefreshToken(userId)

        return AuthTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenResult.refreshToken,
            userId = userId,
        )
    }

    private fun findOrCreateUser(
        socialUserInfo: com.rental.commerce.domain.common.SocialUserInfo,
    ): User {
        val existingBySocial = userRepository.findBySocialProviderAndSocialProviderId(
            socialUserInfo.provider,
            socialUserInfo.socialId,
        )
        if (existingBySocial != null) {
            return existingBySocial
        }

        val existingByEmail = userRepository.findByEmail(socialUserInfo.email)
        if (existingByEmail != null) {
            existingByEmail.linkSocialAccount(socialUserInfo.provider, socialUserInfo.socialId)
            return userRepository.save(existingByEmail)
        }

        val newUser = User.registerSocial(
            email = socialUserInfo.email,
            name = socialUserInfo.name,
            socialProvider = socialUserInfo.provider,
            socialProviderId = socialUserInfo.socialId,
        )
        return userRepository.save(newUser)
    }
}
