package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeDomainService
import com.rental.commerce.domain.dispute.DisputeReason
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

class ResolveDisputeUseCaseTest : BehaviorSpec({

    val disputeDomainService = mockk<DisputeDomainService>()
    val useCase = ResolveDisputeUseCase(disputeDomainService)

    beforeEach { clearMocks(disputeDomainService) }

    fun reviewingDispute() = Dispute.create(
        rentalId = 10L,
        openerId = 7L,
        reason = DisputeReason.DAMAGED,
        description = "설명",
    ).apply {
        startReview()
        resolveFullRefund(BigDecimal("10000"))
    }

    Given("ResolveDisputeUseCase") {
        When("FULL_REFUND 해결") {
            Then("disputeDomainService.resolveFullRefund 호출") {
                val command = ResolveDisputeCommand(
                    disputeId = 1L,
                    type = ResolveDisputeCommand.ResolutionType.FULL_REFUND,
                    refundAmount = BigDecimal("10000"),
                )
                every { disputeDomainService.resolveFullRefund(1L, BigDecimal("10000")) } returns reviewingDispute()
                useCase.execute(command)
                verify(exactly = 1) { disputeDomainService.resolveFullRefund(1L, BigDecimal("10000")) }
            }
        }

        When("PARTIAL 해결") {
            Then("disputeDomainService.resolvePartial 호출") {
                val command = ResolveDisputeCommand(
                    disputeId = 2L,
                    type = ResolveDisputeCommand.ResolutionType.PARTIAL,
                    refundAmount = BigDecimal("3000"),
                )
                every { disputeDomainService.resolvePartial(2L, BigDecimal("3000")) } returns reviewingDispute()
                useCase.execute(command)
                verify(exactly = 1) { disputeDomainService.resolvePartial(2L, BigDecimal("3000")) }
            }
        }

        When("REJECTED 해결") {
            Then("disputeDomainService.resolveRejected 호출 — amount 무시") {
                val command = ResolveDisputeCommand(
                    disputeId = 3L,
                    type = ResolveDisputeCommand.ResolutionType.REJECTED,
                )
                every { disputeDomainService.resolveRejected(3L) } returns reviewingDispute()
                useCase.execute(command)
                verify(exactly = 1) { disputeDomainService.resolveRejected(3L) }
            }
        }

        When("FULL_REFUND 에 refundAmount 없으면 Command 단계에서 차단") {
            Then("IllegalArgumentException 즉시 발생 — UseCase 까지 가지 않음") {
                shouldThrow<IllegalArgumentException> {
                    ResolveDisputeCommand(
                        disputeId = 4L,
                        type = ResolveDisputeCommand.ResolutionType.FULL_REFUND,
                        refundAmount = null,
                    )
                }
            }
        }
    }
})
