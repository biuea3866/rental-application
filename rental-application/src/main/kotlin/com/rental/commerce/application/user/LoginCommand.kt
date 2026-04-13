package com.rental.commerce.application.user

data class LoginCommand(
    val email: String,
    val password: String,
)
