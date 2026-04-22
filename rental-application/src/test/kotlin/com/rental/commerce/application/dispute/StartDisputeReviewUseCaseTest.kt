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

class StartDisputeReviewUseCaseTest : BehaviorSpec({

    val disputeDomainService = mockk<DisputeDomainService>()
    val useCase = StartDisputeReviewUseCase(disputeDomainService)

    beforeEach { clearMocks(disputeDomainService) }

    Given("StartDisputeReviewUseCase") {
        When("관리자가 검토 시작") {
            Then("DomainService.startReview 호출 + UNDER_REVIEW 반환") {
                val reviewing = Dispute.create(
                    rentalId = 10L,
                    openerId = 1L,
                    reason = DisputeReason.DAMAGED,
                    description = "설명",
                ).apply { startReview() }
                every { disputeDomainService.startReview(100L) } returns reviewing

                val result = useCase.execute(100L)

                verify(exactly = 1) { disputeDomainService.startReview(100L) }
                result.status.name shouldBe "UNDER_REVIEW"
            }
        }
    }
})
