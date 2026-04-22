package com.rental.commerce.domain.refund

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class RefundTest : BehaviorSpec({

    fun pendingRefund() = Refund.create(
        paymentId = 10L,
        rentalId = 1L,
        disputeId = 5L,
        amount = BigDecimal("5000"),
        reason = "파손 부분환불",
    )

    Given("Refund.create()") {
        When("amount <= 0") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    Refund.create(1L, 1L, null, BigDecimal.ZERO, "reason")
                }
            }
        }
        When("reason 공백") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    Refund.create(1L, 1L, null, BigDecimal("1000"), "   ")
                }
            }
        }
        When("정상 생성") {
            Then("기본 상태 PENDING") {
                val refund = pendingRefund()
                refund.status shouldBe RefundStatus.PENDING
                refund.processedAt shouldBe null
                refund.externalRefundId shouldBe null
            }
        }
    }

    Given("markSucceeded()") {
        When("PENDING → SUCCEEDED") {
            Then("externalRefundId/processedAt 기록") {
                val refund = pendingRefund()
                refund.markSucceeded("toss-txn-abc")
                refund.status shouldBe RefundStatus.SUCCEEDED
                refund.externalRefundId shouldBe "toss-txn-abc"
                refund.processedAt.shouldNotBeNull()
            }
        }
        When("SUCCEEDED 상태에서 재호출") {
            Then("IllegalStateException") {
                val refund = pendingRefund().apply { markSucceeded("x") }
                shouldThrow<IllegalStateException> { refund.markSucceeded("y") }
            }
        }
    }

    Given("markFailed()") {
        When("PENDING → FAILED") {
            Then("failureReason/processedAt 기록") {
                val refund = pendingRefund()
                refund.markFailed("PG timeout")
                refund.status shouldBe RefundStatus.FAILED
                refund.failureReason shouldBe "PG timeout"
                refund.processedAt.shouldNotBeNull()
            }
        }
        When("FAILED 상태에서 재호출") {
            Then("IllegalStateException") {
                val refund = pendingRefund().apply { markFailed("x") }
                shouldThrow<IllegalStateException> { refund.markFailed("y") }
            }
        }
    }
})

