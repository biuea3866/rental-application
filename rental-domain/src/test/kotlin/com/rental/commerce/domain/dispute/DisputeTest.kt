package com.rental.commerce.domain.dispute

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

/**
 * Dispute Aggregate Root 테스트 (ADR-009).
 */
class DisputeTest : BehaviorSpec({

    fun openDispute() = Dispute.create(
        rentalId = 100L,
        openerId = 1L,
        reason = DisputeReason.DAMAGED,
        description = "제품 파손",
    )

    Given("Dispute.create()") {
        When("정상 생성") {
            val dispute = openDispute()
            Then("상태는 OPEN") { dispute.status shouldBe DisputeStatus.OPEN }
            Then("resolvedAt 은 null") { dispute.resolvedAt shouldBe null }
            Then("refundAmount 은 null") { dispute.refundAmount shouldBe null }
        }
        When("description 이 1000자 초과") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    Dispute.create(1L, 1L, DisputeReason.OTHER, "x".repeat(1001))
                }
            }
        }
        When("description 이 비어있음") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    Dispute.create(1L, 1L, DisputeReason.OTHER, "")
                }
            }
        }
    }

    Given("startReview()") {
        When("OPEN → UNDER_REVIEW") {
            val dispute = openDispute()
            dispute.startReview()
            Then("상태가 UNDER_REVIEW") { dispute.status shouldBe DisputeStatus.UNDER_REVIEW }
        }
        When("이미 UNDER_REVIEW 인 분쟁") {
            val dispute = openDispute().apply { startReview() }
            Then("IllegalStateException") {
                shouldThrow<IllegalStateException> { dispute.startReview() }
            }
        }
    }

    Given("resolveFullRefund()") {
        When("UNDER_REVIEW → RESOLVED_REFUND") {
            val dispute = openDispute().apply { startReview() }
            dispute.resolveFullRefund(BigDecimal("10000"))
            Then("상태 RESOLVED_REFUND") { dispute.status shouldBe DisputeStatus.RESOLVED_REFUND }
            Then("refundAmount 기록") { dispute.refundAmount shouldBe BigDecimal("10000") }
            Then("resolvedAt 기록") { dispute.resolvedAt.shouldNotBeNull() }
        }
        When("OPEN 상태에서 호출") {
            val dispute = openDispute()
            Then("IllegalStateException") {
                shouldThrow<IllegalStateException> { dispute.resolveFullRefund(BigDecimal("10000")) }
            }
        }
    }

    Given("resolvePartial()") {
        When("UNDER_REVIEW → RESOLVED_PARTIAL with amount") {
            val dispute = openDispute().apply { startReview() }
            dispute.resolvePartial(BigDecimal("3000"))
            Then("상태 RESOLVED_PARTIAL") { dispute.status shouldBe DisputeStatus.RESOLVED_PARTIAL }
            Then("refundAmount=3000") { dispute.refundAmount shouldBe BigDecimal("3000") }
        }
        When("refundAmount 가 0 이하") {
            val dispute = openDispute().apply { startReview() }
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { dispute.resolvePartial(BigDecimal.ZERO) }
            }
        }
    }

    Given("resolveRejected()") {
        When("UNDER_REVIEW → RESOLVED_REJECTED") {
            val dispute = openDispute().apply { startReview() }
            dispute.resolveRejected()
            Then("상태 RESOLVED_REJECTED") { dispute.status shouldBe DisputeStatus.RESOLVED_REJECTED }
            Then("refundAmount 은 null") { dispute.refundAmount shouldBe null }
        }
    }

    Given("cancel()") {
        When("opener 본인 + OPEN") {
            val dispute = openDispute()
            dispute.cancel(byUserId = 1L)
            Then("상태 CANCELLED") { dispute.status shouldBe DisputeStatus.CANCELLED }
        }
        When("다른 사용자가 취소 시도") {
            val dispute = openDispute()
            Then("IllegalStateException") {
                shouldThrow<IllegalStateException> { dispute.cancel(byUserId = 999L) }
            }
        }
        When("UNDER_REVIEW 상태에서 취소 시도") {
            val dispute = openDispute().apply { startReview() }
            Then("IllegalStateException") {
                shouldThrow<IllegalStateException> { dispute.cancel(byUserId = 1L) }
            }
        }
    }
})
