package com.rental.commerce.application.user

import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.common.SmsGateway
import com.rental.commerce.domain.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val phoneVerificationStore: PhoneVerificationStore,
    private val smsGateway: SmsGateway,
) {

    companion object {
        private const val VERIFICATION_CODE_LENGTH = 6
        private const val VERIFICATION_CODE_TTL_SECONDS = 300L
        private val secureRandom = SecureRandom()
    }

    fun execute(command: RegisterUserCommand): RegisterUserResponse {
        validateEmailNotDuplicate(command.email)

        val verificationCode = generateVerificationCode()
        phoneVerificationStore.save(command.phone, verificationCode, VERIFICATION_CODE_TTL_SECONDS)
        smsGateway.sendVerificationCode(command.phone, verificationCode)

        return RegisterUserResponse(
            email = command.email,
            phone = command.phone,
            message = "인증코드가 발송되었습니다",
        )
    }

    private fun validateEmailNotDuplicate(email: String) {
        if (userRepository.existsByEmail(email)) {
            throw DuplicateResourceException(
                errorCode = ErrorCode.DUPLICATE_EMAIL,
            )
        }
    }

    private fun generateVerificationCode(): String {
        val code = secureRandom.nextInt(1_000_000)
        return code.toString().padStart(VERIFICATION_CODE_LENGTH, '0')
    }
}
