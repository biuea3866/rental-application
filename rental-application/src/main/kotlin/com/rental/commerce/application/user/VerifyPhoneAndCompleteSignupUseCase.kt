package com.rental.commerce.application.user

import com.rental.commerce.application.auth.RefreshTokenService
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VerifyPhoneAndCompleteSignupUseCase(
    private val userRepository: UserRepository,
    private val phoneVerificationStore: PhoneVerificationStore,
    private val tokenProvider: TokenProvider,
    private val refreshTokenService: RefreshTokenService,
    private val passwordHasher: PasswordHasher,
) {

    @Transactional
    fun execute(command: VerifyPhoneCommand): AuthTokenResponse {
        validateVerificationCode(command.phone, command.code)
        validateEmailNotDuplicate(command.email)

        val user = createUser(command)
        val savedUser = userRepository.save(user)
        val userId = requireNotNull(savedUser.userId) { "저장된 사용자의 ID가 없습니다" }

        phoneVerificationStore.delete(command.phone)

        val accessToken = tokenProvider.createAccessToken(userId, savedUser.role.name)
        val refreshTokenResult = refreshTokenService.issueRefreshToken(userId)

        return AuthTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenResult.refreshToken,
            userId = userId,
        )
    }

    private fun validateVerificationCode(phone: String, code: String) {
        val storedCode = phoneVerificationStore.findByPhone(phone)
            ?: throw BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED)

        if (storedCode != code) {
            throw BusinessException(ErrorCode.INVALID_VERIFICATION_CODE)
        }
    }

    private fun validateEmailNotDuplicate(email: String) {
        if (userRepository.existsByEmail(email)) {
            throw DuplicateResourceException(errorCode = ErrorCode.DUPLICATE_EMAIL)
        }
    }

    private fun createUser(command: VerifyPhoneCommand): User {
        return User(
            email = command.email,
            name = command.name,
            phone = command.phone,
            passwordHash = passwordHasher.hash(command.password),
            role = command.role,
        )
    }
}
