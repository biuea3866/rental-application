package com.rental.commerce.infrastructure.refund

import com.rental.commerce.domain.refund.Refund
import com.rental.commerce.domain.refund.RefundDomainService
import com.rental.commerce.domain.refund.RefundExceedsPaymentException
import com.rental.commerce.domain.refund.RefundRepository
import com.rental.commerce.domain.refund.RefundStatus
import com.rental.commerce.domain.refund.port.PaymentRefundGateway
import com.rental.commerce.domain.refund.port.PaymentRefundResult
import com.rental.commerce.domain.rental.RentalPaymentRepository
import com.rental.commerce.infrastructure.common.config.JpaAuditingConfig
import com.rental.commerce.infrastructure.common.config.QuerydslConfig
import com.rental.commerce.infrastructure.refund.mysql.RefundJpaRepository
import com.rental.commerce.infrastructure.refund.mysql.RefundRepositoryImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.MySQLContainer

/**
 * RC-BE-443 — RefundDomainService @Transactional 명시 통합 테스트.
 *
 * 검증 목표:
 *  1. processRefund 성공 시 Refund 가 DB 에 SUCCEEDED 로 커밋된다.
 *  2. processRefund 누적 초과 예외 시 DB 에 신규 Refund 가 저장되지 않는다 (롤백).
 *  3. PG 예외 발생 시 FAILED Refund 는 DB 에 저장된다 (예외 삼킴 + 커밋).
 *  4. PG 실패 응답 시 FAILED Refund 가 DB 에 저장된다.
 *  5. @Transactional 명시로 호출 측 트랜잭션 없이도 DB 커밋이 보장된다.
 *
 * @Transactional 이 DomainService 에 명시되지 않으면 트랜잭션 경계가 불명확해져
 * 예외 발생 시 부분 저장이 남을 수 있다. 이 테스트는 rollback 동작을 명시적으로 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    value = [
        JpaAuditingConfig::class,
        QuerydslConfig::class,
        RefundRepositoryImpl::class,
    ],
)
@ActiveProfiles("test")
class RefundDomainServiceTransactionalIntegrationTest(
    private val refundJpaRepository: RefundJpaRepository,
    private val refundRepository: RefundRepository,
    private val transactionManager: PlatformTransactionManager,
) : BehaviorSpec({

    extensions(SpringExtension)

    val paymentRefundGateway = mockk<PaymentRefundGateway>()
    val rentalPaymentRepository = mockk<RentalPaymentRepository>(relaxed = true)
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    lateinit var refundDomainService: RefundDomainService

    beforeEach {
        refundJpaRepository.deleteAllInBatch()
        refundDomainService = RefundDomainService(
            refundRepository = refundRepository,
            paymentRefundGateway = paymentRefundGateway,
            rentalPaymentRepository = rentalPaymentRepository,
            eventPublisher = eventPublisher,
        )
    }

    // ── TC-01: 성공 경로 — SUCCEEDED Refund 가 DB 에 커밋된다 ─────────────────

    Given("processRefund — PG 성공") {
        When("유효한 금액으로 환불을 요청하면") {
            Then("SUCCEEDED Refund 가 DB 에 저장된다") {
                every {
                    paymentRefundGateway.requestRefund("pk-success", BigDecimal("5000"), "test-reason")
                } returns PaymentRefundResult.succeeded("ext-refund-001")

                val result = refundDomainService.processRefund(
                    paymentId = 1L,
                    paymentKey = "pk-success",
                    paymentAmount = BigDecimal("10000"),
                    rentalId = 100L,
                    disputeId = null,
                    amount = BigDecimal("5000"),
                    reason = "test-reason",
                )

                result.status shouldBe RefundStatus.SUCCEEDED
                result.externalRefundId shouldBe "ext-refund-001"

                val savedRefunds = refundJpaRepository.findAll()
                savedRefunds.size shouldBe 1
                savedRefunds[0].status shouldBe RefundStatus.SUCCEEDED
                savedRefunds[0].externalRefundId shouldBe "ext-refund-001"
            }
        }
    }

    // ── TC-02: 롤백 동작 — 누적 초과 예외 시 DB 에 신규 Refund 가 저장되지 않는다 ──

    Given("processRefund — 누적 환불 금액 초과") {
        When("이미 8000원 환불된 결제에 3000원을 추가 환불하면") {
            Then("RefundExceedsPaymentException 이 발생하고 DB 에 신규 Refund 가 저장되지 않는다") {
                val existingRefund = Refund.create(
                    paymentId = 2L,
                    rentalId = 200L,
                    disputeId = null,
                    amount = BigDecimal("8000"),
                    reason = "existing-refund",
                )
                val savedExisting = refundJpaRepository.save(existingRefund)
                savedExisting.markSucceeded("ext-existing")
                refundJpaRepository.save(savedExisting)
                refundJpaRepository.flush()

                val beforeCount = refundJpaRepository.findAll().size

                shouldThrow<RefundExceedsPaymentException> {
                    refundDomainService.processRefund(
                        paymentId = 2L,
                        paymentKey = "pk-exceed",
                        paymentAmount = BigDecimal("10000"),
                        rentalId = 200L,
                        disputeId = null,
                        amount = BigDecimal("3000"),
                        reason = "초과 환불 시도",
                    )
                }

                val afterCount = refundJpaRepository.findAll().size
                afterCount shouldBe beforeCount
            }
        }
    }

    // ── TC-03: PG 예외 삼킴 — FAILED Refund 는 DB 에 커밋된다 ──────────────────

    Given("processRefund — PG 예외 발생") {
        When("PG 가 런타임 예외를 던지면") {
            Then("예외가 삼켜지고 FAILED Refund 가 DB 에 저장된다") {
                every {
                    paymentRefundGateway.requestRefund("pk-timeout", any(), any())
                } throws RuntimeException("connection timeout")

                val result = refundDomainService.processRefund(
                    paymentId = 3L,
                    paymentKey = "pk-timeout",
                    paymentAmount = BigDecimal("10000"),
                    rentalId = 300L,
                    disputeId = null,
                    amount = BigDecimal("2000"),
                    reason = "timeout-reason",
                )

                result.status shouldBe RefundStatus.FAILED
                result.failureReason shouldBe "connection timeout"

                val savedRefunds = refundJpaRepository.findAll()
                savedRefunds.size shouldBe 1
                savedRefunds[0].status shouldBe RefundStatus.FAILED
                savedRefunds[0].failureReason shouldBe "connection timeout"
            }
        }
    }

    // ── TC-04: PG 실패 응답 — FAILED Refund 는 DB 에 커밋된다 ─────────────────

    Given("processRefund — PG 실패 응답") {
        When("PG 가 success=false 응답을 반환하면") {
            Then("FAILED Refund 가 DB 에 저장된다") {
                every {
                    paymentRefundGateway.requestRefund("pk-pgfail", any(), any())
                } returns PaymentRefundResult.failed("PG internal error")

                val result = refundDomainService.processRefund(
                    paymentId = 4L,
                    paymentKey = "pk-pgfail",
                    paymentAmount = BigDecimal("10000"),
                    rentalId = 400L,
                    disputeId = 10L,
                    amount = BigDecimal("1500"),
                    reason = "pg-fail-reason",
                )

                result.status shouldBe RefundStatus.FAILED
                result.failureReason shouldBe "PG internal error"

                val savedRefunds = refundJpaRepository.findAll()
                savedRefunds.size shouldBe 1
                savedRefunds[0].status shouldBe RefundStatus.FAILED
                savedRefunds[0].disputeId shouldBe 10L
            }
        }
    }

    // ── TC-05: @Transactional 명시로 호출 측 없이도 커밋 보장 ─────────────────

    Given("processRefund — 트랜잭션 경계 검증") {
        When("외부 트랜잭션 없이 직접 호출해도") {
            Then("DomainService 의 @Transactional 이 트랜잭션을 보장하여 별도 조회 트랜잭션에서도 읽힌다") {
                every {
                    paymentRefundGateway.requestRefund("pk-tx-verify", any(), any())
                } returns PaymentRefundResult.succeeded("ext-tx-001")

                val result = refundDomainService.processRefund(
                    paymentId = 5L,
                    paymentKey = "pk-tx-verify",
                    paymentAmount = BigDecimal("20000"),
                    rentalId = 500L,
                    disputeId = null,
                    amount = BigDecimal("7000"),
                    reason = "tx-verify",
                )

                result.id shouldNotBe 0L
                result.status shouldBe RefundStatus.SUCCEEDED

                // 별도 TransactionTemplate 으로 독립 조회 — 커밋된 데이터가 읽혀야 함
                val transactionTemplate = TransactionTemplate(transactionManager)
                val foundRefund = transactionTemplate.execute {
                    refundJpaRepository.findById(result.id).orElse(null)
                }
                foundRefund shouldNotBe null
                foundRefund!!.status shouldBe RefundStatus.SUCCEEDED
            }
        }
    }
}) {
    companion object {
        private val mysqlContainer = MySQLContainer("mysql:8.0").apply {
            withDatabaseName("rental_commerce_test")
            withUsername("test")
            withPassword("test")
        }

        init {
            mysqlContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { mysqlContainer.driverClassName }
        }
    }
}
