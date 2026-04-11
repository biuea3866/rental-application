package com.rental.commerce.application.user

import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.common.SmsGateway
import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class RegisterUserUseCase(
    private val userDomainService: UserDomainService,
    private val phoneVerificationStore: PhoneVerificationStore,
    private val smsGateway: SmsGateway,
) {

    companion object {
        private const val VERIFICATION_CODE_LENGTH = 6
        private const val VERIFICATION_CODE_TTL_SECONDS = 300L
        private val secureRandom = SecureRandom()
    }

    @Transactional(readOnly = true)
    fun execute(command: RegisterUserCommand): RegisterUserResponse {
        userDomainService.checkEmailNotDuplicate(command.email)

        val verificationCode = generateVerificationCode()
        phoneVerificationStore.save(command.phone, verificationCode, command.email, VERIFICATION_CODE_TTL_SECONDS)
        smsGateway.sendVerificationCode(command.phone, verificationCode)

        return RegisterUserResponse(
            email = command.email,
            phone = command.phone,
            message = "인증코드가 발송되었습니다",
        )
    }

    private fun generateVerificationCode(): String {
        val code = secureRandom.nextInt(1_000_000)
        return code.toString().padStart(VERIFICATION_CODE_LENGTH, '0')
    }
}
