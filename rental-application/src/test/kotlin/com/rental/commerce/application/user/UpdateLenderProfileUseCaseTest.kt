package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.LenderProfile
import com.rental.commerce.domain.user.LenderProfileRepository
import com.rental.commerce.domain.user.LenderType
import com.rental.commerce.domain.user.VerificationStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdateLenderProfileUseCaseTest : BehaviorSpec({

    val lenderProfileRepository = mockk<LenderProfileRepository>()
    val useCase = UpdateLenderProfileUseCase(lenderProfileRepository)

    Given("대여자 프로필 수정 시") {

        When("정산 계좌 정보를 수정하면") {
            val userId = 1L
            val profile = LenderProfile(
                lenderProfileId = 1L,
                userId = userId,
                lenderType = LenderType.INDIVIDUAL,
                verificationStatus = VerificationStatus.VERIFIED,
                settlementAccountBank = "신한은행",
                settlementAccountNumber = "110-123-456789",
            )
            val command = UpdateLenderProfileCommand(
                userId = userId,
                settlementAccountBank = "국민은행",
                settlementAccountNumber = "999-888-777666",
            )

            every { lenderProfileRepository.findByUserId(userId) } returns profile
            every { lenderProfileRepository.save(any()) } returns profile

            val result = useCase.execute(command)

            Then("정산 계좌 은행이 변경된다") {
                result.settlementAccountBank shouldBe "국민은행"
            }

            Then("정산 계좌 번호가 변경된다") {
                result.settlementAccountNumber shouldBe "999-888-777666"
            }

            Then("저장이 호출된다") {
                verify { lenderProfileRepository.save(profile) }
            }
        }

        When("정산 계좌 은행만 수정하면") {
            val userId = 2L
            val profile = LenderProfile(
                lenderProfileId = 2L,
                userId = userId,
                lenderType = LenderType.INDIVIDUAL,
                verificationStatus = VerificationStatus.PENDING,
                settlementAccountBank = "신한은행",
                settlementAccountNumber = "110-123-456789",
            )
            val command = UpdateLenderProfileCommand(
                userId = userId,
                settlementAccountBank = "국민은행",
                settlementAccountNumber = null,
            )

            every { lenderProfileRepository.findByUserId(userId) } returns profile
            every { lenderProfileRepository.save(any()) } returns profile

            val result = useCase.execute(command)

            Then("은행만 변경되고 계좌번호는 유지된다") {
                result.settlementAccountBank shouldBe "국민은행"
                result.settlementAccountNumber shouldBe "110-123-456789"
            }
        }

        When("프로필이 존재하지 않으면") {
            val command = UpdateLenderProfileCommand(
                userId = 999L,
                settlementAccountBank = "국민은행",
                settlementAccountNumber = null,
            )

            every { lenderProfileRepository.findByUserId(999L) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
