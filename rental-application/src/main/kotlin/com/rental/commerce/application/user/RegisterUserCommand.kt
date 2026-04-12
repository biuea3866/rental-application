package com.rental.commerce.application.user

import com.rental.commerce.domain.user.UserRole

data class RegisterUserCommand(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val role: UserRole,
)
