package com.rental.commerce.application.auth

import com.rental.commerce.application.user.AuthTokenResponse
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefreshTokenUseCase(
    private val refreshTokenService: RefreshTokenService,
    private val tokenProvider: TokenProvider,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun execute(command: RefreshTokenCommand): AuthTokenResponse {
        val refreshTokenResult = refreshTokenService.rotateToken(command.tokenFamily, command.refreshToken)

        val user = userRepository.findById(refreshTokenResult.userId)
            ?: throw ResourceNotFoundException(errorCode = ErrorCode.USER_NOT_FOUND)

        val userId = user.requireId()
        val accessToken = tokenProvider.createAccessToken(userId, user.roleName())

        return AuthTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenResult.refreshToken,
            tokenFamily = refreshTokenResult.tokenFamily,
            userId = userId,
        )
    }
}
