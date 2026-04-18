package com.rental.commerce.domain.settlement

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class SettlementTest : BehaviorSpec({

    // ─────────────────────────────────────────────────────────────
    // Settlement.create() — 수수료 자동 계산
    // ─────────────────────────────────────────────────────────────

    Given("Settlement.create() — 수수료 자동 계산") {

        When("amount=50000, commissionRate=0.10으로 create()를 호출하면") {
            val settlement = Settlement.create(
                lenderId = 1L,
                rentalId = 10L,
                amount = BigDecimal("50000"),
                commissionRate = BigDecimal("0.10"),
            )

            Then("commission은 5000이다") {
                settlement.commission shouldBe BigDecimal("5000.00").setScale(2)
            }

            Then("netAmount는 45000이다") {
                settlement.netAmount shouldBe BigDecimal("45000.00").setScale(2)
            }

            Then("status는 PENDING이다") {
                settlement.status shouldBe SettlementStatus.PENDING
            }

            Then("settledAt은 null이다") {
                settlement.settledAt.shouldBeNull()
            }
        }

        When("amount=100000, commissionRate=0.10으로 create()를 호출하면") {
            val settlement = Settlement.create(
                lenderId = 2L,
                rentalId = 20L,
                amount = BigDecimal("100000"),
                commissionRate = BigDecimal("0.10"),
            )

            Then("commission은 10000이다") {
                settlement.commission shouldBe BigDecimal("10000.00").setScale(2)
            }

            Then("netAmount는 90000이다") {
                settlement.netAmount shouldBe BigDecimal("90000.00").setScale(2)
            }
        }

        When("lenderId와 rentalId가 저장된다") {
            val settlement = Settlement.create(
                lenderId = 5L,
                rentalId = 50L,
                amount = BigDecimal("30000"),
                commissionRate = BigDecimal("0.10"),
            )

            Then("lenderId가 저장된다") {
                settlement.lenderId shouldBe 5L
            }

            Then("rentalId가 저장된다") {
                settlement.rentalId shouldBe 50L
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Settlement.complete() — PENDING → COMPLETED 상태 전이
    // ─────────────────────────────────────────────────────────────

    Given("Settlement.complete() — PENDING → COMPLETED 상태 전이") {

        When("PENDING 상태의 Settlement에 complete()를 호출하면") {
            val settlement = Settlement.create(
                lenderId = 1L,
                rentalId = 10L,
                amount = BigDecimal("50000"),
                commissionRate = BigDecimal("0.10"),
            )

            settlement.complete()

            Then("status가 COMPLETED로 변경된다") {
                settlement.status shouldBe SettlementStatus.COMPLETED
            }

            Then("settledAt이 설정된다") {
                settlement.settledAt.shouldNotBeNull()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Settlement.complete() — 이미 처리된 정산 예외
    // ─────────────────────────────────────────────────────────────

    Given("Settlement.complete() — 이미 처리된 정산 예외") {

        When("COMPLETED 상태의 Settlement에 complete()를 다시 호출하면") {
            val settlement = Settlement.create(
                lenderId = 1L,
                rentalId = 10L,
                amount = BigDecimal("50000"),
                commissionRate = BigDecimal("0.10"),
            )
            settlement.complete()

            Then("SettlementAlreadyProcessedException이 발생한다") {
                shouldThrow<SettlementAlreadyProcessedException> {
                    settlement.complete()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SettlementStatus.validateCanComplete()
    // ─────────────────────────────────────────────────────────────

    Given("SettlementStatus.validateCanComplete()") {

        When("PENDING 상태에서 validateCanComplete()를 호출하면") {
            Then("예외 없이 통과한다") {
                SettlementStatus.PENDING.validateCanComplete()
            }
        }

        When("COMPLETED 상태에서 validateCanComplete()를 호출하면") {
            Then("SettlementAlreadyProcessedException이 발생한다") {
                shouldThrow<SettlementAlreadyProcessedException> {
                    SettlementStatus.COMPLETED.validateCanComplete()
                }
            }
        }

        When("CANCELLED 상태에서 validateCanComplete()를 호출하면") {
            Then("SettlementAlreadyProcessedException이 발생한다") {
                shouldThrow<SettlementAlreadyProcessedException> {
                    SettlementStatus.CANCELLED.validateCanComplete()
                }
            }
        }
    }
})
