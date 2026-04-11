package com.rental.commerce.presentation.api.auth

import com.rental.commerce.application.user.AuthTokenResponse
import com.rental.commerce.application.user.RegisterUserResponse
import com.rental.commerce.application.user.RegisterUserUseCase
import com.rental.commerce.application.user.VerifyPhoneAndCompleteSignupUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthApiControllerTest : BehaviorSpec({

    val registerUserUseCase = mockk<RegisterUserUseCase>()
    val verifyPhoneAndCompleteSignupUseCase = mockk<VerifyPhoneAndCompleteSignupUseCase>()
    val controller = AuthApiController(registerUserUseCase, verifyPhoneAndCompleteSignupUseCase)
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    Given("POST /api/v1/auth/signup") {

        When("유효한 정보로 가입 요청하면") {
            every { registerUserUseCase.execute(any()) } returns RegisterUserResponse(
                email = "test@example.com",
                phone = "01012345678",
                message = "인증코드가 발송되었습니다",
            )

            val result = mockMvc.post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "password":"password123!",
                    "name":"홍길동",
                    "phone":"01012345678",
                    "role":"RENTER"
                }"""
            }

            Then("200 OK와 인증코드 발송 메시지가 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.email") { value("test@example.com") }
                    jsonPath("$.phone") { value("01012345678") }
                    jsonPath("$.message") { value("인증코드가 발송되었습니다") }
                }
            }
        }

        When("이미 가입된 이메일로 요청하면") {
            every { registerUserUseCase.execute(any()) } throws BusinessException(
                errorCode = ErrorCode.DUPLICATE_EMAIL,
            )

            val result = mockMvc.post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"existing@example.com",
                    "password":"password123!",
                    "name":"홍길동",
                    "phone":"01012345678",
                    "role":"RENTER"
                }"""
            }

            Then("409 DUPLICATE_EMAIL 에러가 반환된다") {
                result.andExpect {
                    status { isConflict() }
                    jsonPath("$.code") { value("DUPLICATE_EMAIL") }
                }
            }
        }

        When("이메일 형식이 올바르지 않으면") {
            val result = mockMvc.post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"not-an-email",
                    "password":"password123!",
                    "name":"홍길동",
                    "phone":"01012345678",
                    "role":"RENTER"
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("비밀번호가 8자 미만이면") {
            val result = mockMvc.post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "password":"short",
                    "name":"홍길동",
                    "phone":"01012345678",
                    "role":"RENTER"
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }

    Given("POST /api/v1/auth/verify-phone") {

        When("유효한 인증코드로 요청하면") {
            every { verifyPhoneAndCompleteSignupUseCase.execute(any()) } returns AuthTokenResponse(
                accessToken = "access_token_123",
                refreshToken = "refresh_token_123",
                userId = 1L,
            )

            val result = mockMvc.post("/api/v1/auth/verify-phone") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "phone":"01012345678",
                    "code":"123456",
                    "password":"password123!",
                    "name":"홍길동",
                    "role":"RENTER"
                }"""
            }

            Then("200 OK와 JWT 토큰이 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.accessToken") { value("access_token_123") }
                    jsonPath("$.refreshToken") { value("refresh_token_123") }
                    jsonPath("$.userId") { value(1) }
                }
            }
        }

        When("인증코드가 일치하지 않으면") {
            every { verifyPhoneAndCompleteSignupUseCase.execute(any()) } throws BusinessException(
                errorCode = ErrorCode.INVALID_VERIFICATION_CODE,
            )

            val result = mockMvc.post("/api/v1/auth/verify-phone") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "phone":"01012345678",
                    "code":"999999",
                    "password":"password123!",
                    "name":"홍길동",
                    "role":"RENTER"
                }"""
            }

            Then("400 INVALID_VERIFICATION_CODE 에러가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_VERIFICATION_CODE") }
                }
            }
        }

        When("인증코드가 비어있으면") {
            val result = mockMvc.post("/api/v1/auth/verify-phone") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "phone":"01012345678",
                    "code":"",
                    "password":"password123!",
                    "name":"홍길동",
                    "role":"RENTER"
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }
})
