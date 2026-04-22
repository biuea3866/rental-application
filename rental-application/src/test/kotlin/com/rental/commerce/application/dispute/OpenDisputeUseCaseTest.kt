package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeAlreadyActiveException
import com.rental.commerce.domain.dispute.DisputeDomainService
import com.rental.commerce.domain.dispute.DisputeReason
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class OpenDisputeUseCaseTest : BehaviorSpec({

    val disputeDomainService = mockk<DisputeDomainService>()
    val useCase = OpenDisputeUseCase(disputeDomainService)

    beforeEach { clearMocks(disputeDomainService) }

    Given("OpenDisputeUseCase") {
        When("정상 입력으로 분쟁 오픈") {
            Then("domainService.openDispute 가 1회 호출되고 결과가 매핑된다") {
                val command = OpenDisputeCommand(
                    openerId = 1L,
                    rentalId = 100L,
                    reason = DisputeReason.DAMAGED,
                    description = "박스 파손 + 내부 손상",
                )
                val dispute = Dispute.create(
                    rentalId = 100L,
                    openerId = 1L,
                    reason = DisputeReason.DAMAGED,
                    description = "박스 파손 + 내부 손상",
                )
                every {
                    disputeDomainService.openDispute(
                        rentalId = 100L,
                        openerId = 1L,
                        reason = DisputeReason.DAMAGED,
                        description = "박스 파손 + 내부 손상",
                    )
                } returns dispute

                val result = useCase.execute(command)

                verify(exactly = 1) {
                    disputeDomainService.openDispute(
                        rentalId = 100L,
                        openerId = 1L,
                        reason = DisputeReason.DAMAGED,
                        description = "박스 파손 + 내부 손상",
                    )
                }
                result.rentalId shouldBe 100L
                result.openerId shouldBe 1L
            }
        }

        When("도메인 서비스가 DisputeAlreadyActiveException 을 던지면") {
            Then("UseCase 도 그대로 전파한다") {
                val command = OpenDisputeCommand(
                    openerId = 2L,
                    rentalId = 200L,
                    reason = DisputeReason.OTHER,
                    description = "중복 요청",
                )
                every { disputeDomainService.openDispute(any(), any(), any(), any()) } throws
                    DisputeAlreadyActiveException()

                shouldThrow<DisputeAlreadyActiveException> { useCase.execute(command) }
            }
        }
    }
})
