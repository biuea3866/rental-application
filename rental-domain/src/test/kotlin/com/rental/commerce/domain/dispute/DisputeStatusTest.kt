package com.rental.commerce.domain.dispute

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * DisputeStatus 상태 전이 검증 (ADR-009).
 *
 * 전이 규칙:
 *  OPEN → UNDER_REVIEW (startReview)
 *  OPEN → CANCELLED    (opener 자발 취소)
 *  UNDER_REVIEW → RESOLVED_REFUND / RESOLVED_PARTIAL / RESOLVED_REJECTED (resolve)
 *
 * 금지: 종결 상태에서의 모든 전이, OPEN→RESOLVED_*, UNDER_REVIEW→OPEN 등.
 */
class DisputeStatusTest : BehaviorSpec({

    Given("validateCanStartReview()") {
        When("상태가 OPEN") {
            Then("통과") {
                shouldNotThrow<IllegalStateException> {
                    DisputeStatus.OPEN.validateCanStartReview()
                }
            }
        }
        listOf(
            DisputeStatus.UNDER_REVIEW,
            DisputeStatus.RESOLVED_REFUND,
            DisputeStatus.RESOLVED_PARTIAL,
            DisputeStatus.RESOLVED_REJECTED,
            DisputeStatus.CANCELLED,
        ).forEach { status ->
            When("상태가 $status") {
                Then("IllegalStateException") {
                    shouldThrow<IllegalStateException> { status.validateCanStartReview() }
                }
            }
        }
    }

    Given("validateCanResolve()") {
        When("상태가 UNDER_REVIEW") {
            Then("통과") {
                shouldNotThrow<IllegalStateException> {
                    DisputeStatus.UNDER_REVIEW.validateCanResolve()
                }
            }
        }
        listOf(
            DisputeStatus.OPEN,
            DisputeStatus.RESOLVED_REFUND,
            DisputeStatus.RESOLVED_PARTIAL,
            DisputeStatus.RESOLVED_REJECTED,
            DisputeStatus.CANCELLED,
        ).forEach { status ->
            When("상태가 $status") {
                Then("IllegalStateException") {
                    shouldThrow<IllegalStateException> { status.validateCanResolve() }
                }
            }
        }
    }

    Given("validateCanCancel()") {
        When("상태가 OPEN") {
            Then("통과") {
                shouldNotThrow<IllegalStateException> { DisputeStatus.OPEN.validateCanCancel() }
            }
        }
        When("상태가 UNDER_REVIEW — 관리자 검토 시작 후에는 취소 불가") {
            Then("IllegalStateException") {
                shouldThrow<IllegalStateException> { DisputeStatus.UNDER_REVIEW.validateCanCancel() }
            }
        }
    }

    Given("isTerminal()") {
        Then("RESOLVED_* / CANCELLED 은 terminal") {
            DisputeStatus.RESOLVED_REFUND.isTerminal() shouldBe true
            DisputeStatus.RESOLVED_PARTIAL.isTerminal() shouldBe true
            DisputeStatus.RESOLVED_REJECTED.isTerminal() shouldBe true
            DisputeStatus.CANCELLED.isTerminal() shouldBe true
        }
        Then("OPEN / UNDER_REVIEW 는 non-terminal (활성)") {
            DisputeStatus.OPEN.isTerminal() shouldBe false
            DisputeStatus.UNDER_REVIEW.isTerminal() shouldBe false
        }
    }

    Given("isActive() — 활성 분쟁 단일 제약 용도") {
        Then("OPEN / UNDER_REVIEW 만 활성") {
            DisputeStatus.OPEN.isActive() shouldBe true
            DisputeStatus.UNDER_REVIEW.isActive() shouldBe true
            DisputeStatus.RESOLVED_REFUND.isActive() shouldBe false
            DisputeStatus.CANCELLED.isActive() shouldBe false
        }
    }
})
