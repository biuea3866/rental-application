package com.rental.commerce.infrastructure.auth.redis

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer

class RedisRefreshTokenStoreTest : BehaviorSpec({

    val redisContainer = GenericContainer("redis:7-alpine").apply {
        withExposedPorts(6379)
    }

    lateinit var redisTemplate: StringRedisTemplate
    lateinit var store: RedisRefreshTokenStore

    beforeSpec {
        redisContainer.start()

        val redisConfig = RedisStandaloneConfiguration(
            redisContainer.host,
            redisContainer.getMappedPort(6379),
        )
        val connectionFactory = LettuceConnectionFactory(redisConfig)
        connectionFactory.afterPropertiesSet()

        redisTemplate = StringRedisTemplate()
        redisTemplate.connectionFactory = connectionFactory
        redisTemplate.afterPropertiesSet()

        store = RedisRefreshTokenStore(redisTemplate)
    }

    afterSpec {
        redisContainer.stop()
    }

    fun flushRedis() {
        redisTemplate.connectionFactory!!.connection.serverCommands().flushAll()
    }

    Given("RefreshToken 저장 및 조회") {

        When("tokenFamily, refreshToken, userId를 저장하면") {
            flushRedis()

            val tokenFamily = "family-001"
            val refreshToken = "token-abc-123"
            val userId = 42L

            store.save(
                tokenFamily = tokenFamily,
                refreshToken = refreshToken,
                userId = userId,
                expiryDays = 14,
            )

            Then("동일한 tokenFamily로 조회하면 저장한 데이터가 반환된다") {
                val result = store.findByTokenFamily(tokenFamily)

                result shouldNotBe null
                result!!.userId shouldBe userId
                result.refreshToken shouldBe refreshToken
                result.tokenFamily shouldBe tokenFamily
            }
        }

        When("존재하지 않는 tokenFamily로 조회하면") {
            flushRedis()

            Then("null이 반환된다") {
                val result = store.findByTokenFamily("non-existent-family")
                result shouldBe null
            }
        }
    }

    Given("RefreshToken 삭제") {

        When("저장된 tokenFamily를 삭제하면") {
            flushRedis()

            val tokenFamily = "family-delete-test"
            val refreshToken = "token-delete-test"
            val userId = 100L

            store.save(
                tokenFamily = tokenFamily,
                refreshToken = refreshToken,
                userId = userId,
                expiryDays = 14,
            )

            store.deleteByTokenFamily(tokenFamily)

            Then("조회 시 null이 반환된다") {
                val result = store.findByTokenFamily(tokenFamily)
                result shouldBe null
            }
        }

        When("존재하지 않는 tokenFamily를 삭제해도") {
            flushRedis()

            Then("예외가 발생하지 않는다") {
                store.deleteByTokenFamily("non-existent-family")
            }
        }
    }

    Given("Token 사용 여부 마킹 및 확인") {

        When("토큰을 사용됨으로 마킹하면") {
            flushRedis()

            val tokenFamily = "family-used-check"
            val refreshToken = "token-used-check"

            store.markTokenAsUsed(tokenFamily, refreshToken)

            Then("isTokenUsed가 true를 반환한다") {
                val result = store.isTokenUsed(tokenFamily, refreshToken)
                result shouldBe true
            }
        }

        When("마킹하지 않은 토큰을 확인하면") {
            flushRedis()

            Then("isTokenUsed가 false를 반환한다") {
                val result = store.isTokenUsed("family-not-used", "token-not-used")
                result shouldBe false
            }
        }

        When("동일 family에서 서로 다른 토큰을 마킹하면") {
            flushRedis()

            val tokenFamily = "family-multi-token"
            val token1 = "token-first"
            val token2 = "token-second"

            store.markTokenAsUsed(tokenFamily, token1)

            Then("마킹된 토큰은 true를 반환한다") {
                store.isTokenUsed(tokenFamily, token1) shouldBe true
            }

            Then("마킹되지 않은 토큰은 false를 반환한다") {
                store.isTokenUsed(tokenFamily, token2) shouldBe false
            }
        }
    }

    Given("TokenFamily 삭제 시 사용 기록도 함께 삭제") {

        When("토큰을 사용됨으로 마킹한 후 family를 삭제하면") {
            flushRedis()

            val tokenFamily = "family-cascade-delete"
            val refreshToken = "token-cascade"
            val userId = 200L

            store.save(
                tokenFamily = tokenFamily,
                refreshToken = refreshToken,
                userId = userId,
                expiryDays = 14,
            )
            store.markTokenAsUsed(tokenFamily, refreshToken)

            store.deleteByTokenFamily(tokenFamily)

            Then("토큰 데이터가 삭제된다") {
                store.findByTokenFamily(tokenFamily) shouldBe null
            }

            Then("사용 기록도 삭제된다") {
                store.isTokenUsed(tokenFamily, refreshToken) shouldBe false
            }
        }
    }

    Given("Token 덮어쓰기 (Rotation 시나리오)") {

        When("동일 tokenFamily에 새 refreshToken을 저장하면") {
            flushRedis()

            val tokenFamily = "family-overwrite"
            val oldToken = "old-token-value"
            val newToken = "new-token-value"
            val userId = 300L

            store.save(
                tokenFamily = tokenFamily,
                refreshToken = oldToken,
                userId = userId,
                expiryDays = 14,
            )

            store.save(
                tokenFamily = tokenFamily,
                refreshToken = newToken,
                userId = userId,
                expiryDays = 14,
            )

            Then("최신 refreshToken이 조회된다") {
                val result = store.findByTokenFamily(tokenFamily)
                result shouldNotBe null
                result!!.refreshToken shouldBe newToken
                result.userId shouldBe userId
            }
        }
    }
})
