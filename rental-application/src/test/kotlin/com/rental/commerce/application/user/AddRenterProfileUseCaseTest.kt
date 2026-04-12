package com.rental.commerce.application.user

import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.RenterProfileRepository
import com.rental.commerce.domain.user.TrustGrade
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify

class AddRenterProfileUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val renterProfileRepository = mockk<RenterProfileRepository>()
    val userDomainService = mockk<UserDomainService>()
    val useCase = AddRenterProfileUseCase(userRepository, renterProfileRepository, userDomainService)

    Given("대여자 프로필 등록 시") {

        When("유효한 요청으로 등록하면") {
            val userId = 1L
            val command = AddRenterProfileCommand(userId = userId)
            val user = User(email = "test@example.com", name = "테스트", phone = "010-1234-5678", passwordHash = "hashed", role = UserRole.LENDER, id = userId)

            every { userRepository.findById(userId) } returns user
            every { userDomainService.checkRenterProfileNotDuplicate(userId) } just runs
            val profileSlot = slot<RenterProfile>()
            every { renterProfileRepository.save(capture(profileSlot)) } answers { profileSlot.captured }
            every { userRepository.save(any()) } returns user

            val result = useCase.execute(command)

            Then("RenterProfileResponse가 반환된다") {
                result.userId shouldBe userId
                result.trustGrade shouldBe TrustGrade.BRONZE.name
                result.totalTransactionCount shouldBe 0
            }

            Then("사용자 역할이 업데이트된다") {
                verify { userRepository.save(any()) }
            }

            Then("프로필이 저장된다") {
                verify { renterProfileRepository.save(any()) }
            }
        }

        When("존재하지 않는 사용자로 등록하면") {
            val command = AddRenterProfileCommand(userId = 999L)
            every { userRepository.findById(999L) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> { useCase.execute(command) }
            }
        }

        When("이미 대여자 프로필이 존재하면") {
            val userId = 1L
            val command = AddRenterProfileCommand(userId = userId)
            val user = User(email = "test@example.com", name = "테스트", phone = "010-1234-5678", passwordHash = "hashed", role = UserRole.LENDER, id = userId)

            every { userRepository.findById(userId) } returns user
            every { userDomainService.checkRenterProfileNotDuplicate(userId) } throws DuplicateResourceException()

            Then("DuplicateResourceException이 발생한다") {
                shouldThrow<DuplicateResourceException> { useCase.execute(command) }
            }
        }
    }
})
