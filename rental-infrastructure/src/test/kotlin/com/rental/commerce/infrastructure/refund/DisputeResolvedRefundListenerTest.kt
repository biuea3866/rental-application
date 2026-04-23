package com.rental.commerce.infrastructure.refund

import com.rental.commerce.domain.dispute.DisputeStatus
import com.rental.commerce.domain.dispute.event.DisputeResolvedEvent
import com.rental.commerce.domain.refund.RefundDomainService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

/**
 * DisputeResolvedRefundListener 단위 테스트 — Listener 는 얇은 분기만 담당하고
 * 결제 조회 + PG 호출 + 이벤트 발행은 RefundDomainService 로 위임한다 (pr-reviewer 지적 반영, BE-405 refactor).
 */
class DisputeResolvedRefundListenerTest : BehaviorSpec({

    val refundDomainService = mockk<RefundDomainService>(relaxed = true)
    val listener = DisputeResolvedRefundListener(refundDomainService)

    beforeEach { clearMocks(refundDomainService) }

    Given("DisputeResolvedRefundListener") {
        When("RESOLVED_REJECTED 이벤트") {
            Then("환불 대상 아님 — DomainService 미호출") {
                val event = DisputeResolvedEvent(1L, 100L, DisputeStatus.RESOLVED_REJECTED, null)
                listener.handle(event)
                verify(exactly = 0) {
                    refundDomainService.processRefundForRental(any(), any(), any(), any())
                }
            }
        }

        When("CANCELLED 이벤트") {
            Then("스킵") {
                val event = DisputeResolvedEvent(2L, 100L, DisputeStatus.CANCELLED, null)
                listener.handle(event)
                verify(exactly = 0) {
                    refundDomainService.processRefundForRental(any(), any(), any(), any())
                }
            }
        }

        When("RESOLVED_REFUND 이지만 amount null") {
            Then("방어 스킵") {
                val event = DisputeResolvedEvent(3L, 100L, DisputeStatus.RESOLVED_REFUND, null)
                listener.handle(event)
                verify(exactly = 0) {
                    refundDomainService.processRefundForRental(any(), any(), any(), any())
                }
            }
        }

        When("정상 흐름 (PARTIAL 3000)") {
            Then("DomainService.processRefundForRental 호출 — reasonCode 조합 포함") {
                every {
                    refundDomainService.processRefundForRental(
                        rentalId = 700L,
                        disputeId = 5L,
                        amount = BigDecimal("3000"),
                        reasonCode = "DISPUTE_RESOLVED_PARTIAL_5",
                    )
                } returns null // 반환값은 리스너에서 사용하지 않음

                val event = DisputeResolvedEvent(5L, 700L, DisputeStatus.RESOLVED_PARTIAL, BigDecimal("3000"))
                listener.handle(event)

                verify(exactly = 1) {
                    refundDomainService.processRefundForRental(
                        rentalId = 700L,
                        disputeId = 5L,
                        amount = BigDecimal("3000"),
                        reasonCode = "DISPUTE_RESOLVED_PARTIAL_5",
                    )
                }
            }
        }

        When("정상 흐름 (FULL_REFUND 10000)") {
            Then("reasonCode 는 DISPUTE_RESOLVED_REFUND_{id}") {
                val event = DisputeResolvedEvent(7L, 800L, DisputeStatus.RESOLVED_REFUND, BigDecimal("10000"))
                listener.handle(event)

                verify(exactly = 1) {
                    refundDomainService.processRefundForRental(
                        rentalId = 800L,
                        disputeId = 7L,
                        amount = BigDecimal("10000"),
                        reasonCode = "DISPUTE_RESOLVED_REFUND_7",
                    )
                }
            }
        }
    }
})
