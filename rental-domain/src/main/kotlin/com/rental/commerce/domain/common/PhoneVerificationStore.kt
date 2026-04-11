package com.rental.commerce.domain.common

interface PhoneVerificationStore {

    fun save(phone: String, code: String, ttlSeconds: Long = 300)

    fun findByPhone(phone: String): String?

    fun delete(phone: String)
}
