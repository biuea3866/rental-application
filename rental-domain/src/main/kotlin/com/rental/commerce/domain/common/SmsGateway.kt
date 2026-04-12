package com.rental.commerce.domain.common

interface SmsGateway {

    fun sendVerificationCode(phone: String, code: String)
}
