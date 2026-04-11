package com.rental.commerce.infrastructure.sms

import com.rental.commerce.domain.common.SmsGateway
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local", "dev", "test")
class MockSmsGateway : SmsGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(phone: String, code: String) {
        val maskedPhone = phone.replaceRange(3, phone.length - 4, "****")
        logger.info("[MockSMS] 인증코드 발송 - phone: {}, code: ***", maskedPhone)
    }
}
