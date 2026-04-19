package com.rental.commerce.domain.auth

import com.rental.commerce.domain.common.SmsGateway
import com.rental.commerce.domain.common.SocialLoginGateway
import com.rental.commerce.domain.common.SocialUserInfo
import com.rental.commerce.domain.user.SocialProvider
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class AuthDomainService(
    private val smsGateway: SmsGateway,
    private val socialLoginGateway: SocialLoginGateway,
) {

    companion object {
        private const val VERIFICATION_CODE_LENGTH = 6
        private val secureRandom = SecureRandom()
    }

    fun sendPhoneVerificationCode(phone: String): String {
        val code = generateVerificationCode()
        smsGateway.sendVerificationCode(phone, code)
        return code
    }

    fun authenticateWithSocialProvider(
        provider: SocialProvider,
        authorizationCode: String,
    ): SocialUserInfo {
        val providerAccessToken = socialLoginGateway.getAccessToken(provider, authorizationCode)
        return socialLoginGateway.getUserInfo(provider, providerAccessToken)
    }

    private fun generateVerificationCode(): String {
        val code = secureRandom.nextInt(1_000_000)
        return code.toString().padStart(VERIFICATION_CODE_LENGTH, '0')
    }
}
