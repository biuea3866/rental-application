package com.rental.commerce.application.user

import com.rental.commerce.application.auth.RefreshTokenResult
import com.rental.commerce.application.auth.RefreshTokenService
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.common.UnauthorizedException
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class LoginUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val passwordHasher = mockk<PasswordHasher>()
    val tokenProvider = mockk<TokenProvider>()
    val refreshTokenService = mockk<RefreshTokenService>()
    val useCase = LoginUseCase(userRepository, passwordHasher, tokenProvider, refreshTokenService)

    Given("로그인 요청 시") {

        When("유효한 이메일과 비밀번호로 로그인하면") {
            val command = LoginCommand(
                email = "test@example.com",
                password = "password123!",
            )

            val user = User(
                email = "test@example.com",
                name = "홍길동",
                phone = "01012345678",
                passwordHash = "bcrypt_hashed",
                role = UserRole.RENTER,
                id = 1L,
            )

            every { userRepository.findByEmail(command.email) } returns user
            every { passwordHasher.matches("password123!", "bcrypt_hashed") } returns true
            every { tokenProvider.createAccessToken(1L, "RENTER") } returns "access_token_123"
            every { refreshTokenService.issueRefreshToken(1L) } returns RefreshTokenResult(
                refreshToken = "refresh_token_123",
                tokenFamily = "family_123",
                userId = 1L,
            )

            val result = useCase.execute(command)

            Then("Access Token이 반환된다") {
                result.accessToken shouldBe "access_token_123"
            }

            Then("Refresh Token이 반환된다") {
                result.refreshToken shouldBe "refresh_token_123"
            }

            Then("tokenFamily가 반환된다") {
                result.tokenFamily shouldBe "family_123"
            }

            Then("userId가 반환된다") {
                result.userId shouldBe 1L
            }
        }

        When("존재하지 않는 이메일로 로그인하면") {
            val command = LoginCommand(
                email = "notfound@example.com",
                password = "password123!",
            )

            every { userRepository.findByEmail(command.email) } returns null

            Then("USER_NOT_FOUND 에러가 발생한다") {
                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }

        When("비밀번호가 일치하지 않으면") {
            val command = LoginCommand(
                email = "test@example.com",
                password = "wrong_password",
            )

            val user = User(
                email = "test@example.com",
                name = "홍길동",
                phone = "01012345678",
                passwordHash = "bcrypt_hashed",
                role = UserRole.RENTER,
                id = 1L,
            )

            every { userRepository.findByEmail(command.email) } returns user
            every { passwordHasher.matches("wrong_password", "bcrypt_hashed") } returns false

            Then("INVALID_PASSWORD 에러가 발생한다") {
                val exception = shouldThrow<UnauthorizedException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_PASSWORD
            }
        }
    }
})
