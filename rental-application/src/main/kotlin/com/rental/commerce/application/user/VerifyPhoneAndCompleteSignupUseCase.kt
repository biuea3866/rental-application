package com.rental.commerce.application.user

import com.rental.commerce.application.auth.RefreshTokenService
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VerifyPhoneAndCompleteSignupUseCase(
    private val userRepository: UserRepository,
    private val userDomainService: UserDomainService,
    private val phoneVerificationStore: PhoneVerificationStore,
    private val tokenProvider: TokenProvider,
    private val refreshTokenService: RefreshTokenService,
    private val passwordHasher: PasswordHasher,
) {

    @Transactional
    fun execute(command: VerifyPhoneCommand): AuthTokenResponse {
        val verifiedEmail = phoneVerificationStore.verify(command.phone, command.code)

        if (verifiedEmail != command.email) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "인증 요청 이메일과 가입 이메일이 일치하지 않습니다")
        }

        userDomainService.checkEmailNotDuplicate(command.email)

        val user = User.register(
            email = command.email,
            name = command.name,
            phone = command.phone,
            passwordHash = passwordHasher.hash(command.password),
            role = command.role,
        )
        val savedUser = userRepository.save(user)
        val userId = requireNotNull(savedUser.userId) { "저장된 사용자의 ID가 없습니다" }

        phoneVerificationStore.delete(command.phone)

        val accessToken = tokenProvider.createAccessToken(userId, savedUser.role.name)
        val refreshTokenResult = refreshTokenService.issueRefreshToken(userId)

        return AuthTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenResult.refreshToken,
            tokenFamily = refreshTokenResult.tokenFamily,
            userId = userId,
        )
    }
}
