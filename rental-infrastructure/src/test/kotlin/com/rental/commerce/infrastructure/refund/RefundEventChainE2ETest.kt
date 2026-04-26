package com.rental.commerce.infrastructure.refund

import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeReason
import com.rental.commerce.domain.dispute.DisputeStatus
import com.rental.commerce.domain.dispute.event.DisputeResolvedEvent
import com.rental.commerce.domain.refund.RefundDomainService
import com.rental.commerce.domain.refund.RefundRepository
import com.rental.commerce.domain.refund.RefundStatus
import com.rental.commerce.domain.refund.event.RefundCompletedEvent
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.settlement.Settlement
import com.rental.commerce.domain.settlement.SettlementDomainService
import com.rental.commerce.domain.settlement.SettlementRepository
import com.rental.commerce.infrastructure.dispute.mysql.DisputeJpaRepository
import com.rental.commerce.infrastructure.refund.mysql.RefundJpaRepository
import com.rental.commerce.infrastructure.rental.RentalPaymentJpaRepository
import com.rental.commerce.infrastructure.settlement.RefundCompletedSettlementListener
import com.rental.commerce.infrastructure.settlement.mysql.SettlementJpaRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.MySQLContainer
import java.math.BigDecimal

/**
 * RefundEventChainE2ETest — AFTER_COMMIT + REQUIRES_NEW 트랜잭션 경계 E2E 검증.
 *
 * 검증 목표 (ADR-009 §4, BE-404/405):
 *   1. DisputeResolvedEvent(RESOLVED_REFUND) 트랜잭션 커밋 후
 *      → DisputeResolvedRefundListener(AFTER_COMMIT + REQUIRES_NEW) 에서 Refund 엔티티 생성
 *   2. Refund 생성 → RefundDomainService 가 RefundCompletedEvent 발행
 *      → RefundCompletedSettlementListener(AFTER_COMMIT + REQUIRES_NEW) 에서 Settlement.net_amount 보정
 *   3. 트랜잭션 롤백 시 AFTER_COMMIT 리스너 미실행 (이벤트 무시)
 *   4. 동일 RefundCompletedEvent 두 번 처리 시 idempotent — net_amount 결과 동일
 *
 * 왜 @SpringBootTest 풀 컨텍스트인가:
 *   - @DataJpaTest 는 테스트 트랜잭션을 롤백하므로 AFTER_COMMIT 이 절대 실행되지 않음
 *   - TransactionTemplate 으로 명시적 커밋 → AFTER_COMMIT 리스너 실행 보장
 *   - MockPaymentRefundAdapter(@Profile("test")) 가 자동 빈으로 등록됨 (PG Mock)
 *
 * Kotest BehaviorSpec 주의:
 *   - 각 Then 블록은 When 을 재실행하므로 단일 Then 블록에서 모든 검증을 수행한다.
 *   - Given 픽스처는 Given 블록에서 설정, When 에서 액션, Then 에서 통합 검증.
 */
@TestConfiguration
class RefundE2ERedisStubConfig {
    @Bean
    fun stringRedisTemplate(): StringRedisTemplate = mockk(relaxed = true)
}

@SpringBootTest(
    classes = [RefundE2ETestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@Import(value = [RefundE2ERedisStubConfig::class])
@ActiveProfiles("test", "e2e")
class RefundEventChainE2ETest(
    private val eventPublisher: ApplicationEventPublisher,
    private val transactionManager: PlatformTransactionManager,
    private val refundRepository: RefundRepository,
    private val refundJpaRepository: RefundJpaRepository,
    private val settlementRepository: SettlementRepository,
    private val refundCompletedSettlementListener: RefundCompletedSettlementListener,
    private val rentalPaymentJpaRepository: RentalPaymentJpaRepository,
    private val disputeJpaRepository: DisputeJpaRepository,
    private val settlementJpaRepository: SettlementJpaRepository,
) : BehaviorSpec({

    extensions(SpringExtension)

    val transactionTemplate = TransactionTemplate(transactionManager)

    // ─────────────────────────────────────────────────────────────────────────
    // 픽스처 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    fun saveCompletedRentalPayment(
        rentalId: Long,
        amount: Long = 50_000L,
        externalPaymentId: String = "mock-ext-pay-$rentalId",
    ): RentalPayment {
        val payment = RentalPayment.create(
            rentalId = rentalId,
            amount = amount,
            paymentMethod = PaymentMethod.CARD,
            orderId = "RC-$rentalId-${System.currentTimeMillis()}",
        )
        payment.complete(externalPaymentId)
        return rentalPaymentJpaRepository.save(payment)
    }

    fun saveSettlement(
        rentalId: Long,
        lenderId: Long = 99L,
        amount: BigDecimal = BigDecimal("50000"),
        commissionRate: BigDecimal = BigDecimal("0.10"),
    ): Settlement {
        val settlement = Settlement.create(
            lenderId = lenderId,
            rentalId = rentalId,
            amount = amount,
            commissionRate = commissionRate,
        )
        return settlementJpaRepository.save(settlement)
    }

    fun saveUnderReviewDispute(
        rentalId: Long,
        openerId: Long = 1L,
    ): Dispute {
        val dispute = Dispute.create(
            rentalId = rentalId,
            openerId = openerId,
            reason = DisputeReason.DAMAGED,
            description = "E2E 테스트 분쟁 rentalId=$rentalId",
        )
        dispute.startReview()
        return disputeJpaRepository.save(dispute)
    }

    fun publishDisputeResolvedAndCommit(
        disputeId: Long,
        rentalId: Long,
        resolution: DisputeStatus,
        refundAmount: BigDecimal?,
    ) {
        transactionTemplate.execute {
            eventPublisher.publishEvent(
                DisputeResolvedEvent(
                    disputeId = disputeId,
                    rentalId = rentalId,
                    resolution = resolution,
                    refundAmount = refundAmount,
                ),
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 각 시나리오 후 정리
    // ─────────────────────────────────────────────────────────────────────────

    afterEach {
        transactionTemplate.execute {
            refundJpaRepository.deleteAll()
            settlementJpaRepository.deleteAll()
            disputeJpaRepository.deleteAll()
            rentalPaymentJpaRepository.deleteAll()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 시나리오 1: FULL_REFUND — 전체 이벤트 체인 검증
    // ─────────────────────────────────────────────────────────────────────────

    Given("DisputeResolvedEvent(RESOLVED_REFUND) 가 트랜잭션 커밋 후 발행될 때") {

        val rentalId = 1001L
        val paymentAmount = 50_000L
        val refundAmount = BigDecimal("10000")

        saveCompletedRentalPayment(rentalId = rentalId, amount = paymentAmount)
        saveSettlement(rentalId = rentalId, amount = BigDecimal(paymentAmount))
        val dispute = saveUnderReviewDispute(rentalId = rentalId)

        When("DisputeResolvedEvent(RESOLVED_REFUND, 10000) 를 트랜잭션 커밋하면") {

            publishDisputeResolvedAndCommit(
                disputeId = dispute.id,
                rentalId = rentalId,
                resolution = DisputeStatus.RESOLVED_REFUND,
                refundAmount = refundAmount,
            )

            Then(
                "Refund 가 SUCCEEDED 로 생성되고 " +
                    "externalRefundId = mock-refund-* 이며 " +
                    "Settlement net_amount = 35000 이어야 한다",
            ) {
                // Refund 검증
                val refunds = refundRepository.findByDisputeId(dispute.id)
                refunds.size shouldBe 1
                val refund = refunds[0]
                refund.status shouldBe RefundStatus.SUCCEEDED
                refund.rentalId shouldBe rentalId
                // BigDecimal scale 차이 무시 — DB 저장 후 scale 이 맞춰짐(10000.00)
                refund.amount.compareTo(refundAmount) shouldBe 0
                refund.externalRefundId shouldNotBe null
                refund.externalRefundId!!.startsWith("mock-refund-") shouldBe true

                // Settlement 검증 — net = 50000 - 5000 - 10000 = 35000
                val updatedSettlement = settlementRepository.findByRentalId(rentalId)
                updatedSettlement.shouldNotBeNull()
                updatedSettlement.netAmount shouldBe BigDecimal("35000.00")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 시나리오 2: PARTIAL — 부분 환불 이벤트 체인
    // ─────────────────────────────────────────────────────────────────────────

    Given("DisputeResolvedEvent(RESOLVED_PARTIAL) 가 트랜잭션 커밋 후 발행될 때") {

        val rentalId = 1002L
        val paymentAmount = 50_000L
        val partialRefundAmount = BigDecimal("3000")

        saveCompletedRentalPayment(rentalId = rentalId, amount = paymentAmount)
        saveSettlement(rentalId = rentalId, amount = BigDecimal(paymentAmount))
        val dispute = saveUnderReviewDispute(rentalId = rentalId)

        When("DisputeResolvedEvent(RESOLVED_PARTIAL, 3000) 를 트랜잭션 커밋하면") {

            publishDisputeResolvedAndCommit(
                disputeId = dispute.id,
                rentalId = rentalId,
                resolution = DisputeStatus.RESOLVED_PARTIAL,
                refundAmount = partialRefundAmount,
            )

            Then(
                "Refund 가 SUCCEEDED 상태이고 금액=3000 이며 " +
                    "Settlement net_amount = 42000 이어야 한다",
            ) {
                val refunds = refundRepository.findByDisputeId(dispute.id)
                refunds.size shouldBe 1
                refunds[0].status shouldBe RefundStatus.SUCCEEDED
                // BigDecimal scale 차이 무시
                refunds[0].amount.compareTo(partialRefundAmount) shouldBe 0

                val updatedSettlement = settlementRepository.findByRentalId(rentalId)
                updatedSettlement.shouldNotBeNull()
                // net = 50000 - 5000 - 3000 = 42000
                updatedSettlement.netAmount shouldBe BigDecimal("42000.00")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 시나리오 3: REJECTED — 환불 리스너 스킵
    // ─────────────────────────────────────────────────────────────────────────

    Given("DisputeResolvedEvent(RESOLVED_REJECTED) 가 트랜잭션 커밋 후 발행될 때") {

        val rentalId = 1003L
        val paymentAmount = 50_000L

        saveCompletedRentalPayment(rentalId = rentalId, amount = paymentAmount)
        saveSettlement(rentalId = rentalId, amount = BigDecimal(paymentAmount))
        val dispute = saveUnderReviewDispute(rentalId = rentalId)

        When("DisputeResolvedEvent(RESOLVED_REJECTED) 를 트랜잭션 커밋하면") {

            publishDisputeResolvedAndCommit(
                disputeId = dispute.id,
                rentalId = rentalId,
                resolution = DisputeStatus.RESOLVED_REJECTED,
                refundAmount = null,
            )

            Then(
                "Refund 가 생성되지 않고 " +
                    "Settlement net_amount 가 초기값(45000) 그대로여야 한다",
            ) {
                val refunds = refundRepository.findByDisputeId(dispute.id)
                refunds.shouldBeEmpty()

                val settlement = settlementRepository.findByRentalId(rentalId)
                settlement.shouldNotBeNull()
                // 초기 net = 50000 - 5000 = 45000
                settlement.netAmount shouldBe BigDecimal("45000.00")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 시나리오 4: 트랜잭션 롤백 시 AFTER_COMMIT 리스너 미실행
    // ─────────────────────────────────────────────────────────────────────────

    Given("DisputeResolvedEvent 가 발행됐지만 트랜잭션이 롤백될 때") {

        val rentalId = 1004L
        saveCompletedRentalPayment(rentalId = rentalId)
        val dispute = saveUnderReviewDispute(rentalId = rentalId)

        When("이벤트 발행 후 예외가 발생하여 트랜잭션이 롤백되면") {

            runCatching {
                transactionTemplate.execute {
                    eventPublisher.publishEvent(
                        DisputeResolvedEvent(
                            disputeId = dispute.id,
                            rentalId = rentalId,
                            resolution = DisputeStatus.RESOLVED_REFUND,
                            refundAmount = BigDecimal("10000"),
                        ),
                    )
                    // 강제 예외 → 트랜잭션 롤백 → AFTER_COMMIT 이벤트 무시
                    throw RuntimeException("강제 롤백 — AFTER_COMMIT 미실행 검증용")
                }
            }

            Then("AFTER_COMMIT 리스너가 실행되지 않아 Refund 가 생성되지 않아야 한다") {
                val refunds = refundRepository.findByDisputeId(dispute.id)
                refunds.shouldBeEmpty()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 시나리오 5: RefundCompletedSettlementListener idempotent 검증
    // ─────────────────────────────────────────────────────────────────────────

    Given("RefundCompletedEvent(SUCCEEDED) 를 동일 이벤트로 두 번 처리할 때") {

        val rentalId = 1005L
        val paymentAmount = 50_000L
        val refundAmount = BigDecimal("10000")

        saveCompletedRentalPayment(rentalId = rentalId, amount = paymentAmount)
        saveSettlement(rentalId = rentalId, amount = BigDecimal(paymentAmount))
        val dispute = saveUnderReviewDispute(rentalId = rentalId)

        When("FULL_REFUND 이벤트 체인 처리 후 동일 RefundCompletedEvent 를 재수신하면") {

            // 1차 처리 — 전체 이벤트 체인
            publishDisputeResolvedAndCommit(
                disputeId = dispute.id,
                rentalId = rentalId,
                resolution = DisputeStatus.RESOLVED_REFUND,
                refundAmount = refundAmount,
            )

            val netAfterFirstProcess = settlementRepository.findByRentalId(rentalId)!!.netAmount
            val savedRefund = refundRepository.findByDisputeId(dispute.id)[0]

            // 2차: 동일 RefundCompletedEvent 직접 재수신 (중복 이벤트 시나리오)
            transactionTemplate.execute {
                refundCompletedSettlementListener.handle(
                    RefundCompletedEvent(
                        refundId = savedRefund.id,
                        paymentId = savedRefund.paymentId,
                        rentalId = rentalId,
                        disputeId = savedRefund.disputeId,
                        amount = refundAmount,
                        status = RefundStatus.SUCCEEDED,
                    ),
                )
            }

            val netAfterSecondProcess = settlementRepository.findByRentalId(rentalId)!!.netAmount

            Then("1차 처리 후 net=35000 이고 2차 처리 후에도 net=35000 이어야 한다 (idempotent)") {
                // 1차 결과
                netAfterFirstProcess shouldBe BigDecimal("35000.00")
                // 2차 결과 — totalRefunded = SUCCEEDED 합산 = 10000 (중복 없음) → 동일
                netAfterSecondProcess shouldBe BigDecimal("35000.00")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 시나리오 6: RentalPayment 미존재 시 Refund 미생성
    // ─────────────────────────────────────────────────────────────────────────

    Given("RentalPayment 가 없는 rental 에 DisputeResolvedEvent 가 발행될 때") {

        val rentalId = 1006L
        // RentalPayment 저장 안 함 — 방어 케이스
        val dispute = saveUnderReviewDispute(rentalId = rentalId)

        When("FULL_REFUND 이벤트를 트랜잭션 커밋하면") {

            publishDisputeResolvedAndCommit(
                disputeId = dispute.id,
                rentalId = rentalId,
                resolution = DisputeStatus.RESOLVED_REFUND,
                refundAmount = BigDecimal("5000"),
            )

            Then("RentalPayment 미존재로 Refund 가 생성되지 않아야 한다") {
                val refunds = refundRepository.findByDisputeId(dispute.id)
                refunds.shouldBeEmpty()
            }
        }
    }
}) {
    companion object {
        private val mysqlContainer: MySQLContainer<*> = MySQLContainer("mysql:8.0").apply {
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
