package com.rental.commerce.application.user

import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.TrustGrade
import com.rental.commerce.domain.user.UserDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class AddRenterProfileUseCaseTest : BehaviorSpec({

    val userDomainService = mockk<UserDomainService>()
    val useCase = AddRenterProfileUseCase(userDomainService)

    Given("대여자 프로필 등록 시") {

        When("유효한 요청으로 등록하면") {
            val userId = 1L
            val command = AddRenterProfileCommand(userId = userId)

            val savedProfile = RenterProfile(
                renterProfileId = 1L,
                userId = userId,
            )

            every { userDomainService.saveRenterProfileAndGrantRole(userId) } returns savedProfile

            val result = useCase.execute(command)

            Then("RenterProfileResponse가 반환된다") {
                result.userId shouldBe userId
                result.trustGrade shouldBe TrustGrade.BRONZE.name
                result.totalTransactionCount shouldBe 0
            }
        }

        When("존재하지 않는 사용자로 등록하면") {
            val command = AddRenterProfileCommand(userId = 999L)
            every { userDomainService.saveRenterProfileAndGrantRole(999L) } throws
                ResourceNotFoundException()

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> { useCase.execute(command) }
            }
        }

        When("이미 대여자 프로필이 존재하면") {
            val userId = 1L
            val command = AddRenterProfileCommand(userId = userId)

            every { userDomainService.saveRenterProfileAndGrantRole(userId) } throws
                DuplicateResourceException()

            Then("DuplicateResourceException이 발생한다") {
                shouldThrow<DuplicateResourceException> { useCase.execute(command) }
            }
        }
    }
})
