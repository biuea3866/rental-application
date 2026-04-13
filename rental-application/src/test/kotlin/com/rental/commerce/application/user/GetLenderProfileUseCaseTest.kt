package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.LenderProfile
import com.rental.commerce.domain.user.LenderType
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.VerificationStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetLenderProfileUseCaseTest : BehaviorSpec({

    val userDomainService = mockk<UserDomainService>()
    val useCase = GetLenderProfileUseCase(userDomainService)

    Given("대여자 프로필 조회 시") {

        When("프로필이 존재하면") {
            val userId = 1L
            val profile = LenderProfile(
                lenderProfileId = 1L,
                userId = userId,
                lenderType = LenderType.INDIVIDUAL,
                verificationStatus = VerificationStatus.VERIFIED,
                settlementAccountBank = "신한은행",
                settlementAccountNumber = "110-123-456789",
            )

            every { userDomainService.findLenderProfileByUserId(userId) } returns profile

            val result = useCase.execute(userId)

            Then("LenderProfileResponse가 반환된다") {
                result.userId shouldBe userId
                result.lenderType shouldBe LenderType.INDIVIDUAL.name
                result.verificationStatus shouldBe VerificationStatus.VERIFIED.name
                result.settlementAccountBank shouldBe "신한은행"
                result.settlementAccountNumber shouldBe "110-123-456789"
            }
        }

        When("프로필이 존재하지 않으면") {
            val userId = 999L

            every { userDomainService.findLenderProfileByUserId(userId) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(userId)
                }
            }
        }
    }
})
