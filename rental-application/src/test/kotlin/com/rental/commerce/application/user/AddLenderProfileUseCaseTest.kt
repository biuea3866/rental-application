package com.rental.commerce.application.user

import com.rental.commerce.domain.common.DuplicateResourceException
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

class AddLenderProfileUseCaseTest : BehaviorSpec({

    val userDomainService = mockk<UserDomainService>()
    val useCase = AddLenderProfileUseCase(userDomainService)

    Given("등록자 프로필 등록 시") {

        When("유효한 요청으로 등록하면") {
            val userId = 1L
            val command = AddLenderProfileCommand(
                userId = userId,
                lenderType = LenderType.INDIVIDUAL,
            )

            val savedProfile = LenderProfile(
                lenderProfileId = 1L,
                userId = userId,
                lenderType = LenderType.INDIVIDUAL,
            )

            every { userDomainService.saveLenderProfileAndGrantRole(userId, LenderType.INDIVIDUAL) } returns savedProfile

            val result = useCase.execute(command)

            Then("LenderProfileResponse가 반환된다") {
                result.userId shouldBe userId
                result.lenderType shouldBe LenderType.INDIVIDUAL.name
                result.verificationStatus shouldBe VerificationStatus.PENDING.name
            }
        }

        When("존재하지 않는 사용자로 등록하면") {
            val command = AddLenderProfileCommand(userId = 999L, lenderType = LenderType.INDIVIDUAL)
            every { userDomainService.saveLenderProfileAndGrantRole(999L, LenderType.INDIVIDUAL) } throws
                ResourceNotFoundException()

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> { useCase.execute(command) }
            }
        }

        When("이미 등록자 프로필이 존재하면") {
            val userId = 1L
            val command = AddLenderProfileCommand(userId = userId, lenderType = LenderType.INDIVIDUAL)

            every { userDomainService.saveLenderProfileAndGrantRole(userId, LenderType.INDIVIDUAL) } throws
                DuplicateResourceException()

            Then("DuplicateResourceException이 발생한다") {
                shouldThrow<DuplicateResourceException> { useCase.execute(command) }
            }
        }
    }
})
