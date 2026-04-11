package com.rental.commerce.infrastructure.sms

import com.rental.commerce.domain.common.SmsGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MockSmsGateway : SmsGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(phone: String, code: String) {
        logger.info("[MockSMS] 인증코드 발송 - phone: {}, code: {}", phone, code)
    }
}
