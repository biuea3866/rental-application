package com.rental.commerce.application.user

import com.rental.commerce.domain.auth.AuthDomainService
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserUseCase(
    private val userDomainService: UserDomainService,
    private val phoneVerificationStore: PhoneVerificationStore,
    private val authDomainService: AuthDomainService,
) {

    companion object {
        private const val VERIFICATION_CODE_TTL_SECONDS = 300L
    }

    @Transactional(readOnly = true)
    fun execute(command: RegisterUserCommand): RegisterUserResponse {
        userDomainService.checkEmailNotDuplicate(command.email)

        val verificationCode = authDomainService.sendPhoneVerificationCode(command.phone)
        phoneVerificationStore.save(command.phone, verificationCode, command.email, VERIFICATION_CODE_TTL_SECONDS)

        return RegisterUserResponse(
            email = command.email,
            phone = command.phone,
            message = "인증코드가 발송되었습니다",
        )
    }
}
