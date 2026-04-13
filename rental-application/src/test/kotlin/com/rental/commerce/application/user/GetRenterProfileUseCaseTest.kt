package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.TrustGrade
import com.rental.commerce.domain.user.UserDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetRenterProfileUseCaseTest : BehaviorSpec({

    val userDomainService = mockk<UserDomainService>()
    val useCase = GetRenterProfileUseCase(userDomainService)

    Given("임차인 프로필 조회 시") {

        When("프로필이 존재하면") {
            val userId = 1L
            val profile = RenterProfile(
                renterProfileId = 1L,
                userId = userId,
                trustGrade = TrustGrade.SILVER,
                totalTransactionCount = 15,
            )

            every { userDomainService.findRenterProfileByUserId(userId) } returns profile

            val result = useCase.execute(userId)

            Then("RenterProfileResponse가 반환된다") {
                result.userId shouldBe userId
                result.trustGrade shouldBe TrustGrade.SILVER.name
                result.totalTransactionCount shouldBe 15
            }
        }

        When("프로필이 존재하지 않으면") {
            val userId = 999L

            every { userDomainService.findRenterProfileByUserId(userId) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(userId)
                }
            }
        }
    }
})
