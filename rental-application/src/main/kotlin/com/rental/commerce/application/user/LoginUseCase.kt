package com.rental.commerce.application.user

import com.rental.commerce.application.auth.RefreshTokenService
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginUseCase(
    private val userDomainService: UserDomainService,
    private val passwordHasher: PasswordHasher,
    private val tokenProvider: TokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional(readOnly = true)
    fun execute(command: LoginCommand): AuthTokenResponse {
        val user = userDomainService.findByEmail(command.email)

        user.verifyPassword(command.password, passwordHasher)

        val userId = user.requireId()
        val accessToken = tokenProvider.createAccessToken(userId, user.roleName())
        val refreshTokenResult = refreshTokenService.issueRefreshToken(userId)

        return AuthTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenResult.refreshToken,
            tokenFamily = refreshTokenResult.tokenFamily,
            userId = userId,
        )
    }
}
