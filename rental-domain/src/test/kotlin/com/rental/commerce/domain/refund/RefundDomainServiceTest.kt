package com.rental.commerce.domain.refund

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.refund.port.PaymentRefundGateway
import com.rental.commerce.domain.refund.port.PaymentRefundResult
import com.rental.commerce.domain.rental.RentalPaymentRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import org.springframework.context.ApplicationEventPublisher

class RefundDomainServiceTest : BehaviorSpec({

    val refundRepository = mockk<RefundRepository>()
    val paymentGateway = mockk<PaymentRefundGateway>()
    val rentalPaymentRepository = mockk<RentalPaymentRepository>(relaxed = true)
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    val service = RefundDomainService(
        refundRepository,
        paymentGateway,
        rentalPaymentRepository,
        eventPublisher,
    )

    Given("processRefund") {
        When("누적 환불 금액이 원 결제 초과") {
            Then("RefundExceedsPaymentException — errorCode.code = REFUND_EXCEEDS_PAYMENT") {
                every { refundRepository.sumNonFailedAmountByPaymentId(1L) } returns BigDecimal("8000")
                val exception = shouldThrow<RefundExceedsPaymentException> {
                    service.processRefund(
                        paymentId = 1L, paymentKey = "pk", paymentAmount = BigDecimal("10000"),
                        rentalId = 100L, disputeId = null, amount = BigDecimal("3000"),
                        reason = "초과 시나리오",
                    )
                }
                exception.errorCode shouldBe ErrorCode.REFUND_EXCEEDS_PAYMENT
                exception.errorCode.code shouldBe "REFUND_EXCEEDS_PAYMENT"
                exception.errorCode.httpStatus shouldBe 400
            }
        }

        When("PG 호출 성공") {
            Then("Refund 저장 2번(PENDING → SUCCEEDED) + gateway 1번 호출") {
                every { refundRepository.sumNonFailedAmountByPaymentId(2L) } returns BigDecimal.ZERO
                val savedSlot = slot<Refund>()
                every { refundRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
                every { paymentGateway.requestRefund("pk2", BigDecimal("5000"), "test") } returns
                    PaymentRefundResult.succeeded("ext-123")

                val result = service.processRefund(
                    paymentId = 2L, paymentKey = "pk2", paymentAmount = BigDecimal("10000"),
                    rentalId = 100L, disputeId = 7L, amount = BigDecimal("5000"),
                    reason = "test",
                )

                verify(exactly = 2) { refundRepository.save(any()) }
                verify(exactly = 1) { paymentGateway.requestRefund("pk2", BigDecimal("5000"), "test") }
                result.status shouldBe RefundStatus.SUCCEEDED
                result.externalRefundId shouldBe "ext-123"
            }
        }

        When("PG 호출 실패 응답") {
            Then("FAILED 로 마킹 + 예외 전파 없음") {
                every { refundRepository.sumNonFailedAmountByPaymentId(3L) } returns BigDecimal.ZERO
                val savedSlot = slot<Refund>()
                every { refundRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
                every { paymentGateway.requestRefund(any(), any(), any()) } returns
                    PaymentRefundResult.failed("PG down")

                val result = service.processRefund(
                    paymentId = 3L, paymentKey = "pk3", paymentAmount = BigDecimal("10000"),
                    rentalId = 200L, disputeId = null, amount = BigDecimal("1000"),
                    reason = "test",
                )

                result.status shouldBe RefundStatus.FAILED
                result.failureReason shouldBe "PG down"
            }
        }

        When("PG 호출이 예외를 던짐") {
            Then("FAILED 로 마킹 + 예외 삼킴(runCatching)") {
                every { refundRepository.sumNonFailedAmountByPaymentId(4L) } returns BigDecimal.ZERO
                val savedSlot = slot<Refund>()
                every { refundRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
                every { paymentGateway.requestRefund(any(), any(), any()) } throws
                    RuntimeException("timeout")

                val result = service.processRefund(
                    paymentId = 4L, paymentKey = "pk4", paymentAmount = BigDecimal("10000"),
                    rentalId = 300L, disputeId = null, amount = BigDecimal("500"),
                    reason = "test",
                )

                result.status shouldBe RefundStatus.FAILED
                result.failureReason shouldBe "timeout"
            }
        }
    }
})
