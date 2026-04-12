package com.rental.commerce.domain.common

interface PasswordHasher {

    fun hash(rawPassword: String): String

    fun matches(rawPassword: String, hashedPassword: String): Boolean
}
