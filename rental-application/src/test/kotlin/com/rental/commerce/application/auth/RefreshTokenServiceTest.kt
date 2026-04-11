package com.rental.commerce.application.auth

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ExpiredTokenException
import com.rental.commerce.domain.common.InvalidTokenException
import com.rental.commerce.domain.common.RefreshTokenData
import com.rental.commerce.domain.common.RefreshTokenStore
import com.rental.commerce.domain.common.TokenFamilyCompromisedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class RefreshTokenServiceTest : BehaviorSpec({

    val refreshTokenStore = mockk<RefreshTokenStore>()
    val refreshTokenService = RefreshTokenService(refreshTokenStore)

    Given("Refresh Token 발급") {

        When("userId로 새 Refresh Token을 발급하면") {
            val userId = 1L

            every { refreshTokenStore.save(any(), any(), eq(userId), any()) } just Runs

            val result = refreshTokenService.issueRefreshToken(userId)

            Then("빈 문자열이 아닌 refreshToken이 반환된다") {
                result.refreshToken.shouldNotBeBlank()
            }

            Then("빈 문자열이 아닌 tokenFamily가 반환된다") {
                result.tokenFamily.shouldNotBeBlank()
            }

            Then("RefreshTokenStore에 저장이 호출된다") {
                verify(exactly = 1) {
                    refreshTokenStore.save(
                        tokenFamily = result.tokenFamily,
                        refreshToken = result.refreshToken,
                        userId = userId,
                        expiryDays = any(),
                    )
                }
            }
        }
    }

    Given("Refresh Token Rotation") {

        When("유효한 refreshToken과 tokenFamily로 rotation하면") {
            val userId = 1L
            val oldTokenFamily = "old-family-id"
            val oldRefreshToken = "old-refresh-token"

            val storedData = RefreshTokenData(
                userId = userId,
                refreshToken = oldRefreshToken,
                tokenFamily = oldTokenFamily,
            )

            every { refreshTokenStore.findByTokenFamily(oldTokenFamily) } returns storedData
            every { refreshTokenStore.isTokenUsed(oldTokenFamily, oldRefreshToken) } returns false
            every { refreshTokenStore.markTokenAsUsed(oldTokenFamily, oldRefreshToken) } just Runs
            every { refreshTokenStore.save(eq(oldTokenFamily), any(), eq(userId), any()) } just Runs

            val result = refreshTokenService.rotateToken(oldTokenFamily, oldRefreshToken)

            Then("새로운 refreshToken이 반환된다") {
                result.refreshToken.shouldNotBeBlank()
                result.refreshToken shouldNotBe oldRefreshToken
            }

            Then("동일한 tokenFamily가 유지된다") {
                result.tokenFamily shouldBe oldTokenFamily
            }

            Then("이전 토큰이 사용됨으로 마킹된다") {
                verify(exactly = 1) {
                    refreshTokenStore.markTokenAsUsed(oldTokenFamily, oldRefreshToken)
                }
            }

            Then("새 토큰이 동일 family에 저장된다") {
                verify(exactly = 1) {
                    refreshTokenStore.save(
                        tokenFamily = oldTokenFamily,
                        refreshToken = result.refreshToken,
                        userId = userId,
                        expiryDays = any(),
                    )
                }
            }
        }

        When("존재하지 않는 tokenFamily로 rotation을 시도하면") {
            val tokenFamily = "non-existent-family"
            val refreshToken = "some-token"

            every { refreshTokenStore.findByTokenFamily(tokenFamily) } returns null

            Then("InvalidTokenException이 발생한다") {
                val exception = shouldThrow<InvalidTokenException> {
                    refreshTokenService.rotateToken(tokenFamily, refreshToken)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_TOKEN
            }
        }

        When("저장된 refreshToken과 다른 토큰으로 rotation을 시도하면") {
            val tokenFamily = "valid-family"
            val wrongRefreshToken = "wrong-token"

            val storedData = RefreshTokenData(
                userId = 1L,
                refreshToken = "correct-token",
                tokenFamily = tokenFamily,
            )

            every { refreshTokenStore.findByTokenFamily(tokenFamily) } returns storedData
            every { refreshTokenStore.isTokenUsed(tokenFamily, wrongRefreshToken) } returns false

            Then("InvalidTokenException이 발생한다") {
                val exception = shouldThrow<InvalidTokenException> {
                    refreshTokenService.rotateToken(tokenFamily, wrongRefreshToken)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_TOKEN
            }
        }
    }

    Given("Token Family Detection (재사용 감지)") {

        When("이미 사용된 refreshToken으로 rotation을 시도하면") {
            val tokenFamily = "compromised-family"
            val usedRefreshToken = "used-token"

            val storedData = RefreshTokenData(
                userId = 1L,
                refreshToken = usedRefreshToken,
                tokenFamily = tokenFamily,
            )

            every { refreshTokenStore.findByTokenFamily(tokenFamily) } returns storedData
            every { refreshTokenStore.isTokenUsed(tokenFamily, usedRefreshToken) } returns true
            every { refreshTokenStore.deleteByTokenFamily(tokenFamily) } just Runs

            Then("TokenFamilyCompromisedException이 발생한다") {
                val exception = shouldThrow<TokenFamilyCompromisedException> {
                    refreshTokenService.rotateToken(tokenFamily, usedRefreshToken)
                }
                exception.errorCode shouldBe ErrorCode.TOKEN_FAMILY_COMPROMISED
            }

            Then("해당 tokenFamily가 전체 삭제된다") {
                // 위 shouldThrow에서 이미 호출되었으므로 별도 호출 필요 없음
                verify(atLeast = 1) {
                    refreshTokenStore.deleteByTokenFamily(tokenFamily)
                }
            }
        }
    }

    Given("Refresh Token 폐기") {

        When("tokenFamily로 토큰을 폐기하면") {
            val tokenFamily = "revoke-target-family"

            every { refreshTokenStore.deleteByTokenFamily(tokenFamily) } just Runs

            refreshTokenService.revokeTokenFamily(tokenFamily)

            Then("RefreshTokenStore에서 삭제가 호출된다") {
                verify(exactly = 1) {
                    refreshTokenStore.deleteByTokenFamily(tokenFamily)
                }
            }
        }
    }
})
