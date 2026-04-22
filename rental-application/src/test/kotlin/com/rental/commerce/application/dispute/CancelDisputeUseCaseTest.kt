package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeDomainService
import com.rental.commerce.domain.dispute.DisputeReason
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CancelDisputeUseCaseTest : BehaviorSpec({

    val disputeDomainService = mockk<DisputeDomainService>()
    val useCase = CancelDisputeUseCase(disputeDomainService)

    beforeEach { clearMocks(disputeDomainService) }

    Given("CancelDisputeUseCase") {
        When("opener 본인 취소") {
            Then("DomainService.cancelByOpener 가 호출되고 CANCELLED 반환") {
                val cancelled = Dispute.create(
                    rentalId = 100L,
                    openerId = 1L,
                    reason = DisputeReason.OTHER,
                    description = "오픈 후 철회",
                ).apply { cancel(byUserId = 1L) }

                every { disputeDomainService.cancelByOpener(55L, 1L) } returns cancelled

                val result = useCase.execute(disputeId = 55L, requesterId = 1L)

                verify(exactly = 1) { disputeDomainService.cancelByOpener(55L, 1L) }
                result.status.name shouldBe "CANCELLED"
            }
        }
    }
})
