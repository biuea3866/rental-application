package com.rental.commerce.infrastructure.settlement

import com.rental.commerce.domain.refund.RefundRepository
import com.rental.commerce.domain.refund.RefundStatus
import com.rental.commerce.domain.refund.event.RefundCompletedEvent
import com.rental.commerce.domain.settlement.Settlement
import com.rental.commerce.domain.settlement.SettlementDomainService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

class RefundCompletedSettlementListenerTest : BehaviorSpec({

    val settlementDomainService = mockk<SettlementDomainService>()
    val refundRepository = mockk<RefundRepository>()
    val listener = RefundCompletedSettlementListener(settlementDomainService, refundRepository)

    Given("RefundCompletedSettlementListener") {
        When("FAILED 이벤트") {
            Then("처리 스킵") {
                val event = RefundCompletedEvent(
                    refundId = 1L, paymentId = 10L, rentalId = 100L,
                    disputeId = 5L, amount = BigDecimal("1000"), status = RefundStatus.FAILED,
                )
                listener.handle(event)
                verify(exactly = 0) { refundRepository.sumSucceededAmountByRentalId(any()) }
            }
        }

        When("SUCCEEDED + Settlement 존재") {
            Then("net_amount 재계산 호출 + totalRefunded 전달") {
                val settlement = mockk<Settlement>()
                every { settlement.id } returns 42L
                every { settlement.netAmount } returns BigDecimal("42000.00")

                every { refundRepository.sumSucceededAmountByRentalId(200L) } returns BigDecimal("3000")
                every { settlementDomainService.applyRefundAdjustment(200L, BigDecimal("3000")) } returns settlement

                val event = RefundCompletedEvent(
                    refundId = 2L, paymentId = 20L, rentalId = 200L,
                    disputeId = 5L, amount = BigDecimal("3000"), status = RefundStatus.SUCCEEDED,
                )
                listener.handle(event)

                verify(exactly = 1) { refundRepository.sumSucceededAmountByRentalId(200L) }
                verify(exactly = 1) { settlementDomainService.applyRefundAdjustment(200L, BigDecimal("3000")) }
            }
        }

        When("SUCCEEDED + Settlement 미존재") {
            Then("로그 + 스킵") {
                every { refundRepository.sumSucceededAmountByRentalId(300L) } returns BigDecimal("500")
                every { settlementDomainService.applyRefundAdjustment(300L, BigDecimal("500")) } returns null

                val event = RefundCompletedEvent(
                    refundId = 3L, paymentId = 30L, rentalId = 300L,
                    disputeId = null, amount = BigDecimal("500"), status = RefundStatus.SUCCEEDED,
                )
                listener.handle(event)

                verify(exactly = 1) { settlementDomainService.applyRefundAdjustment(300L, BigDecimal("500")) }
            }
        }
    }
})
