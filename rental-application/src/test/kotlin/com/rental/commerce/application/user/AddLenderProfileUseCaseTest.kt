package com.rental.commerce.application.user

import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.LenderProfile
import com.rental.commerce.domain.user.LenderProfileRepository
import com.rental.commerce.domain.user.LenderType
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import com.rental.commerce.domain.user.VerificationStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify

class AddLenderProfileUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val lenderProfileRepository = mockk<LenderProfileRepository>()
    val userDomainService = mockk<UserDomainService>()
    val useCase = AddLenderProfileUseCase(userRepository, lenderProfileRepository, userDomainService)

    Given("등록자 프로필 등록 시") {

        When("유효한 요청으로 등록하면") {
            val userId = 1L
            val command = AddLenderProfileCommand(
                userId = userId,
                lenderType = LenderType.INDIVIDUAL,
            )
            val user = User(
                email = "test@example.com",
                name = "테스트",
                phone = "010-1234-5678",
                passwordHash = "hashed",
                role = UserRole.RENTER,
                userId = userId,
            )

            every { userRepository.findById(userId) } returns user
            every { userDomainService.checkLenderProfileNotDuplicate(userId) } just runs
            val profileSlot = slot<LenderProfile>()
            every { lenderProfileRepository.save(capture(profileSlot)) } answers {
                profileSlot.captured
            }
            every { userRepository.save(any()) } returns user

            val result = useCase.execute(command)

            Then("LenderProfileResponse가 반환된다") {
                result.userId shouldBe userId
                result.lenderType shouldBe LenderType.INDIVIDUAL.name
                result.verificationStatus shouldBe VerificationStatus.PENDING.name
            }

            Then("사용자 역할이 업데이트된다") {
                verify { userRepository.save(any()) }
            }

            Then("프로필이 저장된다") {
                verify { lenderProfileRepository.save(any()) }
            }
        }

        When("존재하지 않는 사용자로 등록하면") {
            val command = AddLenderProfileCommand(userId = 999L, lenderType = LenderType.INDIVIDUAL)
            every { userRepository.findById(999L) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> { useCase.execute(command) }
            }
        }

        When("이미 등록자 프로필이 존재하면") {
            val userId = 1L
            val command = AddLenderProfileCommand(userId = userId, lenderType = LenderType.INDIVIDUAL)
            val user = User(email = "test@example.com", name = "테스트", phone = "010-1234-5678", passwordHash = "hashed", role = UserRole.RENTER, userId = userId)

            every { userRepository.findById(userId) } returns user
            every { userDomainService.checkLenderProfileNotDuplicate(userId) } throws DuplicateResourceException()

            Then("DuplicateResourceException이 발생한다") {
                shouldThrow<DuplicateResourceException> { useCase.execute(command) }
            }
        }
    }
})
