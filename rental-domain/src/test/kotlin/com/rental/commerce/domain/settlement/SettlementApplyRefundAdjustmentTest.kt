package com.rental.commerce.domain.settlement

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

/**
 * Settlement.applyRefundAdjustment (BE-406, ADR-009 §4) 단위 테스트.
 */
class SettlementApplyRefundAdjustmentTest : BehaviorSpec({

    fun newSettlement(amount: BigDecimal = BigDecimal("50000"), rate: BigDecimal = BigDecimal("0.10")) =
        Settlement.create(
            lenderId = 1L,
            rentalId = 100L,
            amount = amount,
            commissionRate = rate,
        )

    Given("applyRefundAdjustment(totalRefunded)") {
        When("환불 없음 (totalRefunded=0)") {
            Then("netAmount 는 초기값과 동일 (amount - commission)") {
                val s = newSettlement() // amount=50000 commission=5000 netAmount=45000
                s.applyRefundAdjustment(BigDecimal.ZERO)
                s.netAmount shouldBe BigDecimal("45000.00")
            }
        }
        When("부분 환불 3000") {
            Then("netAmount = 50000 - 5000 - 3000 = 42000") {
                val s = newSettlement()
                s.applyRefundAdjustment(BigDecimal("3000"))
                s.netAmount shouldBe BigDecimal("42000.00")
            }
        }
        When("중복 보정 호출 (idempotent)") {
            Then("같은 totalRefunded 이면 결과 동일") {
                val s = newSettlement()
                s.applyRefundAdjustment(BigDecimal("3000"))
                s.applyRefundAdjustment(BigDecimal("3000"))
                s.netAmount shouldBe BigDecimal("42000.00")
            }
        }
        When("환불 총액이 수수료 제외 기준금액을 초과") {
            Then("IllegalArgumentException") {
                val s = newSettlement() // base = 50000 - 5000 = 45000
                shouldThrow<IllegalArgumentException> {
                    s.applyRefundAdjustment(BigDecimal("45001"))
                }
            }
        }
        When("음수 totalRefunded") {
            Then("IllegalArgumentException") {
                val s = newSettlement()
                shouldThrow<IllegalArgumentException> {
                    s.applyRefundAdjustment(BigDecimal("-1"))
                }
            }
        }
    }
})
