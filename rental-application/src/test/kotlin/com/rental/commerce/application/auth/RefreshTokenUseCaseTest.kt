package com.rental.commerce.application.auth

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidTokenException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.common.TokenFamilyCompromisedException
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class RefreshTokenUseCaseTest : BehaviorSpec({

    val refreshTokenService = mockk<RefreshTokenService>()
    val tokenProvider = mockk<TokenProvider>()
    val userRepository = mockk<UserRepository>()
    val useCase = RefreshTokenUseCase(refreshTokenService, tokenProvider, userRepository)

    Given("토큰 갱신 요청 시") {

        When("유효한 Refresh Token으로 갱신하면") {
            val command = RefreshTokenCommand(
                refreshToken = "old-refresh-token",
                tokenFamily = "family-id",
            )

            val user = User(
                email = "test@example.com",
                name = "홍길동",
                phone = "01012345678",
                passwordHash = "bcrypt_hashed",
                role = UserRole.RENTER,
                id = 1L,
            )

            every { refreshTokenService.rotateToken("family-id", "old-refresh-token") } returns RefreshTokenResult(
                refreshToken = "new-refresh-token",
                tokenFamily = "family-id",
                userId = 1L,
            )
            every { userRepository.findById(1L) } returns user
            every { tokenProvider.createAccessToken(1L, "RENTER") } returns "new-access-token"

            val result = useCase.execute(command)

            Then("새로운 Access Token이 반환된다") {
                result.accessToken shouldBe "new-access-token"
            }

            Then("새로운 Refresh Token이 반환된다") {
                result.refreshToken shouldBe "new-refresh-token"
            }

            Then("동일한 tokenFamily가 반환된다") {
                result.tokenFamily shouldBe "family-id"
            }

            Then("userId가 반환된다") {
                result.userId shouldBe 1L
            }
        }

        When("유효하지 않은 Refresh Token으로 갱신하면") {
            val command = RefreshTokenCommand(
                refreshToken = "invalid-token",
                tokenFamily = "family-id",
            )

            every { refreshTokenService.rotateToken("family-id", "invalid-token") } throws
                InvalidTokenException("유효하지 않은 리프레시 토큰입니다")

            Then("INVALID_TOKEN 에러가 발생한다") {
                val exception = shouldThrow<InvalidTokenException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_TOKEN
            }
        }

        When("유효한 Refresh Token이지만 해당 사용자가 존재하지 않으면") {
            val command = RefreshTokenCommand(
                refreshToken = "valid-refresh-token",
                tokenFamily = "family-id",
            )

            every { refreshTokenService.rotateToken("family-id", "valid-refresh-token") } returns RefreshTokenResult(
                refreshToken = "new-refresh-token",
                tokenFamily = "family-id",
                userId = 999L,
            )
            every { userRepository.findById(999L) } returns null

            Then("USER_NOT_FOUND 에러가 발생한다") {
                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }

        When("이미 사용된 Refresh Token으로 갱신하면 (탈취 감지)") {
            val command = RefreshTokenCommand(
                refreshToken = "used-token",
                tokenFamily = "compromised-family",
            )

            every { refreshTokenService.rotateToken("compromised-family", "used-token") } throws
                TokenFamilyCompromisedException("토큰 재사용이 감지되었습니다. 토큰 패밀리가 무효화되었습니다")

            Then("TOKEN_FAMILY_COMPROMISED 에러가 발생한다") {
                val exception = shouldThrow<TokenFamilyCompromisedException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.TOKEN_FAMILY_COMPROMISED
            }
        }
    }
})
