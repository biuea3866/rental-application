package com.rental.commerce.domain.common

interface PhoneVerificationStore {

    fun save(phone: String, code: String, email: String, ttlSeconds: Long = 300)

    fun verify(phone: String, code: String): String

    fun delete(phone: String)
}
