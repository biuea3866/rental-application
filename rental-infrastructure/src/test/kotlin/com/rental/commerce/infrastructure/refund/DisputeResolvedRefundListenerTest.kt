package com.rental.commerce.infrastructure.refund

import com.rental.commerce.domain.dispute.DisputeStatus
import com.rental.commerce.domain.dispute.event.DisputeResolvedEvent
import com.rental.commerce.domain.refund.Refund
import com.rental.commerce.domain.refund.RefundDomainService
import com.rental.commerce.domain.refund.event.RefundCompletedEvent
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal

/**
 * Listener 단위 테스트 (MockK) — AFTER_COMMIT / REQUIRES_NEW 자체는 Spring 설정 검증 불필요.
 * 통합 테스트는 BE-404+BE-405 머지 이후 별도 커밋에서 Testcontainers 로 진행.
 */
class DisputeResolvedRefundListenerTest : BehaviorSpec({

    val refundDomainService = mockk<RefundDomainService>()
    val rentalPaymentRepository = mockk<RentalPaymentRepository>()
    val eventPublisher = mockk<ApplicationEventPublisher>()
    val listener = DisputeResolvedRefundListener(
        refundDomainService, rentalPaymentRepository, eventPublisher,
    )

    fun rentalPaymentStub(extKey: String? = "toss-pay-abc"): RentalPayment {
        val payment = mockk<RentalPayment>()
        every { payment.id } returns 99L
        every { payment.amount } returns 10000L
        every { payment.externalPaymentId } returns extKey
        return payment
    }

    Given("DisputeResolvedRefundListener") {
        When("RESOLVED_REJECTED 이벤트") {
            Then("환불 스킵 — DomainService 미호출") {
                val event = DisputeResolvedEvent(1L, 100L, DisputeStatus.RESOLVED_REJECTED, null)
                listener.handle(event)
                verify(exactly = 0) { refundDomainService.processRefund(any(), any(), any(), any(), any(), any(), any()) }
            }
        }

        When("refundAmount 이 null 인 RESOLVED_REFUND") {
            Then("방어적 스킵") {
                val event = DisputeResolvedEvent(2L, 100L, DisputeStatus.RESOLVED_REFUND, null)
                listener.handle(event)
                verify(exactly = 0) { refundDomainService.processRefund(any(), any(), any(), any(), any(), any(), any()) }
            }
        }

        When("rental_payment 존재하지 않음") {
            Then("오류 로그 + 스킵") {
                every { rentalPaymentRepository.findByRentalId(500L) } returns null
                val event = DisputeResolvedEvent(3L, 500L, DisputeStatus.RESOLVED_REFUND, BigDecimal("1000"))
                listener.handle(event)
                verify(exactly = 0) { refundDomainService.processRefund(any(), any(), any(), any(), any(), any(), any()) }
            }
        }

        When("externalPaymentId 누락") {
            Then("스킵") {
                every { rentalPaymentRepository.findByRentalId(600L) } returns rentalPaymentStub(extKey = null)
                val event = DisputeResolvedEvent(4L, 600L, DisputeStatus.RESOLVED_REFUND, BigDecimal("1000"))
                listener.handle(event)
                verify(exactly = 0) { refundDomainService.processRefund(any(), any(), any(), any(), any(), any(), any()) }
            }
        }

        When("정상 흐름 (PARTIAL 3000)") {
            Then("DomainService 호출 + RefundCompletedEvent 발행") {
                every { rentalPaymentRepository.findByRentalId(700L) } returns rentalPaymentStub()
                val refund = mockk<Refund>()
                every { refund.id } returns 42L
                every { refund.paymentId } returns 99L
                every { refund.rentalId } returns 700L
                every { refund.disputeId } returns 5L
                every { refund.amount } returns BigDecimal("3000")
                every { refund.status } returns com.rental.commerce.domain.refund.RefundStatus.SUCCEEDED
                every {
                    refundDomainService.processRefund(
                        paymentId = 99L,
                        paymentKey = "toss-pay-abc",
                        paymentAmount = BigDecimal.valueOf(10000),
                        rentalId = 700L,
                        disputeId = 5L,
                        amount = BigDecimal("3000"),
                        reason = "DISPUTE_RESOLVED_PARTIAL_5",
                    )
                } returns refund
                every { eventPublisher.publishEvent(any<RefundCompletedEvent>()) } just Runs

                val event = DisputeResolvedEvent(5L, 700L, DisputeStatus.RESOLVED_PARTIAL, BigDecimal("3000"))
                listener.handle(event)

                verify(exactly = 1) {
                    refundDomainService.processRefund(
                        paymentId = 99L,
                        paymentKey = "toss-pay-abc",
                        paymentAmount = BigDecimal.valueOf(10000),
                        rentalId = 700L,
                        disputeId = 5L,
                        amount = BigDecimal("3000"),
                        reason = "DISPUTE_RESOLVED_PARTIAL_5",
                    )
                }
                verify(exactly = 1) { eventPublisher.publishEvent(any<RefundCompletedEvent>()) }
            }
        }
    }
})
