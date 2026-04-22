package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeDomainService
import com.rental.commerce.domain.dispute.DisputeForbiddenException
import com.rental.commerce.domain.dispute.DisputeReason
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetDisputeUseCaseTest : BehaviorSpec({

    val disputeDomainService = mockk<DisputeDomainService>()
    val useCase = GetDisputeUseCase(disputeDomainService)

    beforeEach { clearMocks(disputeDomainService) }

    fun stubDispute(openerId: Long) = Dispute.create(
        rentalId = 10L,
        openerId = openerId,
        reason = DisputeReason.DAMAGED,
        description = "설명",
    )

    Given("GetDisputeUseCase") {
        When("요청자가 opener 본인") {
            Then("정상 조회 (200)") {
                every { disputeDomainService.getById(1L) } returns stubDispute(openerId = 7L)
                val result = useCase.execute(disputeId = 1L, requesterId = 7L, isAdmin = false)
                result.openerId shouldBe 7L
            }
        }

        When("요청자가 타 유저 + isAdmin=false") {
            Then("DisputeForbiddenException") {
                every { disputeDomainService.getById(2L) } returns stubDispute(openerId = 7L)
                shouldThrow<DisputeForbiddenException> {
                    useCase.execute(disputeId = 2L, requesterId = 999L, isAdmin = false)
                }
            }
        }

        When("요청자가 타 유저 + isAdmin=true") {
            Then("관리자 권한으로 정상 조회") {
                every { disputeDomainService.getById(3L) } returns stubDispute(openerId = 7L)
                val result = useCase.execute(disputeId = 3L, requesterId = 999L, isAdmin = true)
                result.openerId shouldBe 7L
            }
        }
    }
})
