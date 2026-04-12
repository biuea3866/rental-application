package com.rental.commerce.presentation.api.auth

import com.rental.commerce.application.user.LoginCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,
) {
    fun toCommand(): LoginCommand = LoginCommand(
        email = email,
        password = password,
    )
}
