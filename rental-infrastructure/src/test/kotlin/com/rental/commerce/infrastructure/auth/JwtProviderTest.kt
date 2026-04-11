package com.rental.commerce.infrastructure.auth

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ExpiredTokenException
import com.rental.commerce.domain.common.InvalidTokenException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.security.KeyPairGenerator
import java.time.Duration

class JwtProviderTest : BehaviorSpec({

    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    val jwtProperties = JwtProperties(
        accessTokenExpiry = Duration.ofMinutes(30),
        refreshTokenExpiry = Duration.ofDays(14),
        issuer = "rental-commerce-test",
    )

    val jwtProvider = JwtProvider(
        jwtProperties = jwtProperties,
        privateKey = keyPair.private,
        publicKey = keyPair.public,
    )

    Given("JwtProvider가 초기화되었을 때") {

        When("Access Token을 생성하면") {
            val userId = 1L
            val role = "LENDER"
            val token = jwtProvider.createAccessToken(userId = userId, role = role)

            Then("빈 문자열이 아닌 유효한 토큰이 반환된다") {
                token.shouldNotBeBlank()
            }

            Then("토큰을 검증하면 userId와 role이 정확히 추출된다") {
                val claims = jwtProvider.validateAccessToken(token)
                claims.userId shouldBe userId
                claims.role shouldBe role
            }

            Then("토큰의 issuedAt과 expiresAt이 올바르게 설정된다") {
                val claims = jwtProvider.validateAccessToken(token)
                claims.issuedAt shouldNotBe null
                claims.expiresAt shouldNotBe null
                val durationSeconds = Duration.between(claims.issuedAt, claims.expiresAt).seconds
                durationSeconds shouldBe jwtProperties.accessTokenExpiry.seconds
            }
        }

        When("다른 userId와 role로 Access Token을 생성하면") {
            val userId = 999L
            val role = "RENTER"
            val token = jwtProvider.createAccessToken(userId = userId, role = role)

            Then("해당 userId와 role이 정확히 추출된다") {
                val claims = jwtProvider.validateAccessToken(token)
                claims.userId shouldBe userId
                claims.role shouldBe role
            }
        }

        When("만료된 Access Token으로 검증하면") {
            val expiredProperties = JwtProperties(
                accessTokenExpiry = Duration.ofSeconds(0),
                refreshTokenExpiry = Duration.ofDays(14),
                issuer = "rental-commerce-test",
            )
            val expiredJwtProvider = JwtProvider(
                jwtProperties = expiredProperties,
                privateKey = keyPair.private,
                publicKey = keyPair.public,
            )

            // 만료 시간을 0초로 설정한 토큰 생성
            val expiredToken = expiredJwtProvider.createAccessToken(userId = 1L, role = "LENDER")

            // 토큰이 만료되도록 약간 대기
            Thread.sleep(100)

            Then("ExpiredTokenException이 발생한다") {
                val exception = shouldThrow<ExpiredTokenException> {
                    jwtProvider.validateAccessToken(expiredToken)
                }
                exception.errorCode shouldBe ErrorCode.EXPIRED_TOKEN
            }
        }

        When("변조된 Access Token으로 검증하면") {
            val validToken = jwtProvider.createAccessToken(userId = 1L, role = "LENDER")
            val tamperedToken = validToken.dropLast(5) + "XXXXX"

            Then("InvalidTokenException이 발생한다") {
                val exception = shouldThrow<InvalidTokenException> {
                    jwtProvider.validateAccessToken(tamperedToken)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_TOKEN
            }
        }

        When("완전히 잘못된 형식의 토큰으로 검증하면") {
            val malformedToken = "this.is.not.a.valid.jwt"

            Then("InvalidTokenException이 발생한다") {
                val exception = shouldThrow<InvalidTokenException> {
                    jwtProvider.validateAccessToken(malformedToken)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_TOKEN
            }
        }

        When("다른 키로 서명된 토큰으로 검증하면") {
            val anotherKeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val anotherJwtProvider = JwtProvider(
                jwtProperties = jwtProperties,
                privateKey = anotherKeyPair.private,
                publicKey = anotherKeyPair.public,
            )
            val tokenFromAnotherProvider = anotherJwtProvider.createAccessToken(userId = 1L, role = "LENDER")

            Then("InvalidTokenException이 발생한다") {
                val exception = shouldThrow<InvalidTokenException> {
                    jwtProvider.validateAccessToken(tokenFromAnotherProvider)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_TOKEN
            }
        }

        When("빈 문자열 토큰으로 검증하면") {
            val emptyToken = ""

            Then("InvalidTokenException이 발생한다") {
                val exception = shouldThrow<InvalidTokenException> {
                    jwtProvider.validateAccessToken(emptyToken)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_TOKEN
            }
        }
    }
})
