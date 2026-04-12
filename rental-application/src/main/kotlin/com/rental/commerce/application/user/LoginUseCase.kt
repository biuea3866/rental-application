package com.rental.commerce.application.user

import com.rental.commerce.application.auth.RefreshTokenService
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenProvider: TokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional(readOnly = true)
    fun execute(command: LoginCommand): AuthTokenResponse {
        val user = userRepository.findByEmail(command.email)
            ?: throw ResourceNotFoundException(errorCode = ErrorCode.USER_NOT_FOUND)

        user.verifyPassword(command.password, passwordHasher)

        val userId = requireNotNull(user.userId) { "사용자 ID가 존재하지 않습니다" }
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
