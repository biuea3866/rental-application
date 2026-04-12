package com.rental.commerce.application.user

import com.rental.commerce.domain.user.UserRole

data class VerifyPhoneCommand(
    val email: String,
    val phone: String,
    val code: String,
    val password: String,
    val name: String,
    val role: UserRole,
)
