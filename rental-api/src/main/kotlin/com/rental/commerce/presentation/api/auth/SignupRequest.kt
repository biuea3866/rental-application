package com.rental.commerce.presentation.api.auth

import com.rental.commerce.application.user.RegisterUserCommand
import com.rental.commerce.domain.user.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 50, message = "비밀번호는 8~50자여야 합니다")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,

    @field:NotBlank(message = "전화번호는 필수입니다")
    @field:Pattern(regexp = "^01[016789]\\d{7,8}$", message = "전화번호 형식이 올바르지 않습니다")
    val phone: String,

    val role: UserRole = UserRole.RENTER,
) {
    fun toCommand(): RegisterUserCommand = RegisterUserCommand(
        email = email,
        password = password,
        name = name,
        phone = phone,
        role = role,
    )
}
