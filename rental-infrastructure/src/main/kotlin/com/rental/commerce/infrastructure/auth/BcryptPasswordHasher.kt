package com.rental.commerce.infrastructure.auth

import com.rental.commerce.domain.common.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordHasher : PasswordHasher {

    private val encoder = BCryptPasswordEncoder(12)

    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    override fun matches(rawPassword: String, hashedPassword: String): Boolean =
        encoder.matches(rawPassword, hashedPassword)
}
