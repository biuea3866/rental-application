package com.rental.commerce.presentation.api.auth

import com.rental.commerce.application.auth.RefreshTokenUseCase
import com.rental.commerce.application.auth.SocialLoginUseCase
import com.rental.commerce.application.user.AuthTokenResponse
import com.rental.commerce.application.user.LoginUseCase
import com.rental.commerce.application.user.RegisterUserResponse
import com.rental.commerce.application.user.RegisterUserUseCase
import com.rental.commerce.application.user.VerifyPhoneAndCompleteSignupUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidTokenException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.common.TokenFamilyCompromisedException
import com.rental.commerce.domain.common.UnauthorizedException
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
    val loginUseCase = mockk<LoginUseCase>()
    val refreshTokenUseCase = mockk<RefreshTokenUseCase>()
    val socialLoginUseCase = mockk<SocialLoginUseCase>()
    val controller = AuthApiController(registerUserUseCase, verifyPhoneAndCompleteSignupUseCase, loginUseCase, refreshTokenUseCase, socialLoginUseCase)
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
                tokenFamily = "family_123",
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

    Given("POST /api/v1/auth/login") {

        When("유효한 이메일과 비밀번호로 로그인하면") {
            every { loginUseCase.execute(any()) } returns AuthTokenResponse(
                accessToken = "access_token_123",
                refreshToken = "refresh_token_123",
                tokenFamily = "family_123",
                userId = 1L,
            )

            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "password":"password123!"
                }"""
            }

            Then("200 OK와 토큰이 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.accessToken") { value("access_token_123") }
                    jsonPath("$.refreshToken") { value("refresh_token_123") }
                    jsonPath("$.tokenFamily") { value("family_123") }
                    jsonPath("$.userId") { value(1) }
                }
            }
        }

        When("존재하지 않는 이메일로 로그인하면") {
            every { loginUseCase.execute(any()) } throws ResourceNotFoundException(
                errorCode = ErrorCode.USER_NOT_FOUND,
            )

            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"notfound@example.com",
                    "password":"password123!"
                }"""
            }

            Then("404 USER_NOT_FOUND 에러가 반환된다") {
                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("USER_NOT_FOUND") }
                }
            }
        }

        When("비밀번호가 일치하지 않으면") {
            every { loginUseCase.execute(any()) } throws UnauthorizedException(
                errorCode = ErrorCode.INVALID_PASSWORD,
            )

            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "password":"wrong_password"
                }"""
            }

            Then("401 INVALID_PASSWORD 에러가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.code") { value("INVALID_PASSWORD") }
                }
            }
        }

        When("이메일 형식이 올바르지 않으면") {
            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"not-an-email",
                    "password":"password123!"
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("비밀번호가 비어있으면") {
            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "email":"test@example.com",
                    "password":""
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }

    Given("POST /api/v1/auth/refresh") {

        When("유효한 Refresh Token으로 갱신하면") {
            every { refreshTokenUseCase.execute(any()) } returns AuthTokenResponse(
                accessToken = "new_access_token",
                refreshToken = "new_refresh_token",
                tokenFamily = "family_123",
                userId = 1L,
            )

            val result = mockMvc.post("/api/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "refreshToken":"old_refresh_token",
                    "tokenFamily":"family_123"
                }"""
            }

            Then("200 OK와 새 토큰이 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.accessToken") { value("new_access_token") }
                    jsonPath("$.refreshToken") { value("new_refresh_token") }
                    jsonPath("$.tokenFamily") { value("family_123") }
                    jsonPath("$.userId") { value(1) }
                }
            }
        }

        When("유효하지 않은 Refresh Token으로 갱신하면") {
            every { refreshTokenUseCase.execute(any()) } throws InvalidTokenException(
                "유효하지 않은 리프레시 토큰입니다",
            )

            val result = mockMvc.post("/api/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "refreshToken":"invalid_token",
                    "tokenFamily":"family_123"
                }"""
            }

            Then("401 INVALID_TOKEN 에러가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.code") { value("INVALID_TOKEN") }
                }
            }
        }

        When("토큰 탈취가 감지되면") {
            every { refreshTokenUseCase.execute(any()) } throws TokenFamilyCompromisedException(
                "토큰 재사용이 감지되었습니다. 토큰 패밀리가 무효화되었습니다",
            )

            val result = mockMvc.post("/api/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "refreshToken":"used_token",
                    "tokenFamily":"compromised_family"
                }"""
            }

            Then("401 TOKEN_FAMILY_COMPROMISED 에러가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.code") { value("TOKEN_FAMILY_COMPROMISED") }
                }
            }
        }

        When("refreshToken이 비어있으면") {
            val result = mockMvc.post("/api/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "refreshToken":"",
                    "tokenFamily":"family_123"
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("tokenFamily가 비어있으면") {
            val result = mockMvc.post("/api/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "refreshToken":"some_token",
                    "tokenFamily":""
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }

    Given("POST /api/v1/auth/social-login") {

        When("유효한 소셜 로그인 요청이면") {
            every { socialLoginUseCase.execute(any()) } returns AuthTokenResponse(
                accessToken = "social_access_token",
                refreshToken = "social_refresh_token",
                userId = 10L,
            )

            val result = mockMvc.post("/api/v1/auth/social-login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "provider":"KAKAO",
                    "authorizationCode":"valid_code_123"
                }"""
            }

            Then("200 OK와 JWT 토큰이 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.accessToken") { value("social_access_token") }
                    jsonPath("$.refreshToken") { value("social_refresh_token") }
                    jsonPath("$.userId") { value(10) }
                }
            }
        }

        When("provider가 비어있으면") {
            val result = mockMvc.post("/api/v1/auth/social-login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "provider":"",
                    "authorizationCode":"valid_code_123"
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("authorizationCode가 비어있으면") {
            val result = mockMvc.post("/api/v1/auth/social-login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "provider":"KAKAO",
                    "authorizationCode":""
                }"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("외부 API 호출에 실패하면") {
            every { socialLoginUseCase.execute(any()) } throws BusinessException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
            )

            val result = mockMvc.post("/api/v1/auth/social-login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                    "provider":"NAVER",
                    "authorizationCode":"invalid_code"
                }"""
            }

            Then("500 EXTERNAL_API_ERROR가 반환된다") {
                result.andExpect {
                    status { isInternalServerError() }
                    jsonPath("$.code") { value("EXTERNAL_API_ERROR") }
                }
            }
        }
    }
})
