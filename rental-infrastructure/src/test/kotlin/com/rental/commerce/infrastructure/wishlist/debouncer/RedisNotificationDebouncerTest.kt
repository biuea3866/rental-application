package com.rental.commerce.infrastructure.wishlist.debouncer

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer

/**
 * RedisNotificationDebouncer 통합 테스트 (RC-DEVOPS-446).
 *
 * Testcontainers Redis를 사용하여 실제 SETNX + TTL 동작을 검증한다.
 */
class RedisNotificationDebouncerTest : BehaviorSpec({

    val redisContainer = GenericContainer<Nothing>("redis:7-alpine").apply {
        withExposedPorts(6379)
    }

    lateinit var redisTemplate: StringRedisTemplate
    lateinit var debouncer: RedisNotificationDebouncer

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

        debouncer = RedisNotificationDebouncer(redisTemplate)
    }

    afterSpec {
        redisContainer.stop()
    }

    fun flushRedis() {
        requireNotNull(redisTemplate.connectionFactory).connection.serverCommands().flushAll()
    }

    Given("RedisNotificationDebouncer — 기본 debounce 동작") {

        When("특정 userId + productId 조합으로 첫 번째 tryAcquire 호출 시") {
            flushRedis()

            val userId = 1L
            val productId = 100L

            val result = debouncer.tryAcquire(userId, productId, Duration.ofMinutes(10))

            Then("true를 반환한다 (발송 허용)") {
                result shouldBe true
            }
        }

        When("동일한 userId + productId 조합으로 두 번째 tryAcquire 호출 시 (TTL 내)") {
            flushRedis()

            val userId = 1L
            val productId = 100L

            debouncer.tryAcquire(userId, productId, Duration.ofMinutes(10))
            val result = debouncer.tryAcquire(userId, productId, Duration.ofMinutes(10))

            Then("false를 반환한다 (debounce 적용 — 스킵)") {
                result shouldBe false
            }
        }
    }

    Given("RedisNotificationDebouncer — TTL 만료 후 동작") {

        When("TTL이 매우 짧게 설정된 후 만료되면") {
            flushRedis()

            val userId = 2L
            val productId = 200L

            debouncer.tryAcquire(userId, productId, Duration.ofMillis(100))

            Thread.sleep(200)

            val result = debouncer.tryAcquire(userId, productId, Duration.ofMillis(100))

            Then("true를 반환한다 (debounce 해제 — 발송 허용)") {
                result shouldBe true
            }
        }
    }

    Given("RedisNotificationDebouncer — 키 독립성 검증") {

        When("userId가 다른 경우") {
            flushRedis()

            val productId = 300L

            debouncer.tryAcquire(10L, productId, Duration.ofMinutes(10))
            val result = debouncer.tryAcquire(11L, productId, Duration.ofMinutes(10))

            Then("독립적으로 true를 반환한다 (다른 사용자는 debounce 영향 없음)") {
                result shouldBe true
            }
        }

        When("productId가 다른 경우") {
            flushRedis()

            val userId = 20L

            debouncer.tryAcquire(userId, 400L, Duration.ofMinutes(10))
            val result = debouncer.tryAcquire(userId, 401L, Duration.ofMinutes(10))

            Then("독립적으로 true를 반환한다 (다른 상품은 debounce 영향 없음)") {
                result shouldBe true
            }
        }

        When("여러 userId + productId 조합이 혼재할 때") {
            flushRedis()

            // userId=30, productId=500 debounce 적용
            debouncer.tryAcquire(30L, 500L, Duration.ofMinutes(10))

            // userId=31, productId=500 첫 호출
            val resultDifferentUser = debouncer.tryAcquire(31L, 500L, Duration.ofMinutes(10))
            // userId=30, productId=501 첫 호출
            val resultDifferentProduct = debouncer.tryAcquire(30L, 501L, Duration.ofMinutes(10))
            // userId=30, productId=500 두 번째 호출 → debounce
            val resultSame = debouncer.tryAcquire(30L, 500L, Duration.ofMinutes(10))

            Then("다른 userId는 true, 다른 productId는 true, 동일 조합은 false") {
                resultDifferentUser shouldBe true
                resultDifferentProduct shouldBe true
                resultSame shouldBe false
            }
        }
    }

    Given("RedisNotificationDebouncer — Redis 키 네이밍 검증") {

        When("tryAcquire 호출 후") {
            flushRedis()

            val userId = 50L
            val productId = 600L

            debouncer.tryAcquire(userId, productId, Duration.ofMinutes(5))

            Then("Redis에 예상된 키 포맷으로 값이 저장된다") {
                val key = "debounce:notification:$userId:$productId"
                val value = redisTemplate.opsForValue().get(key)
                value shouldBe "1"
            }
        }
    }
})
