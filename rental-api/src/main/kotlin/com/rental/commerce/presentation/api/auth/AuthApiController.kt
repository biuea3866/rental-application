package com.rental.commerce.presentation.api.auth

import com.rental.commerce.application.auth.SocialLoginUseCase
import com.rental.commerce.application.user.AuthTokenResponse
import com.rental.commerce.application.user.RegisterUserResponse
import com.rental.commerce.application.user.RegisterUserUseCase
import com.rental.commerce.application.user.VerifyPhoneAndCompleteSignupUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthApiController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val verifyPhoneAndCompleteSignupUseCase: VerifyPhoneAndCompleteSignupUseCase,
    private val socialLoginUseCase: SocialLoginUseCase,
) {

    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest,
    ): ResponseEntity<RegisterUserResponse> {
        val response = registerUserUseCase.execute(request.toCommand())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/verify-phone")
    fun verifyPhone(
        @Valid @RequestBody request: VerifyPhoneRequest,
    ): ResponseEntity<AuthTokenResponse> {
        val response = verifyPhoneAndCompleteSignupUseCase.execute(request.toCommand())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/social-login")
    fun socialLogin(
        @Valid @RequestBody request: SocialLoginRequest,
    ): ResponseEntity<AuthTokenResponse> {
        val response = socialLoginUseCase.execute(request.toCommand())
        return ResponseEntity.ok(response)
    }
}
