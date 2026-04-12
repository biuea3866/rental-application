package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdateUserProfileUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val useCase = UpdateUserProfileUseCase(userRepository)

    Given("사용자 기본 정보 수정 시") {

        When("이름과 전화번호를 수정하면") {
            val userId = 1L
            val user = User(
                email = "test@example.com",
                name = "홍길동",
                phone = "010-1234-5678",
                passwordHash = "hashed",
                role = UserRole.RENTER,
                userId = userId,
            )
            val command = UpdateUserProfileCommand(
                userId = userId,
                name = "김철수",
                phone = "010-9999-8888",
            )

            every { userRepository.findById(userId) } returns user
            every { userRepository.save(any()) } returns user

            useCase.execute(command)

            Then("이름이 변경된다") {
                user.name shouldBe "김철수"
            }

            Then("전화번호가 변경된다") {
                user.phone shouldBe "010-9999-8888"
            }

            Then("저장이 호출된다") {
                verify { userRepository.save(user) }
            }
        }

        When("이름만 수정하면") {
            val userId = 2L
            val user = User(
                email = "test2@example.com",
                name = "원래이름",
                phone = "010-1111-2222",
                passwordHash = "hashed",
                role = UserRole.RENTER,
                userId = userId,
            )
            val command = UpdateUserProfileCommand(
                userId = userId,
                name = "새이름",
                phone = null,
            )

            every { userRepository.findById(userId) } returns user
            every { userRepository.save(any()) } returns user

            useCase.execute(command)

            Then("이름만 변경되고 전화번호는 유지된다") {
                user.name shouldBe "새이름"
                user.phone shouldBe "010-1111-2222"
            }
        }

        When("존재하지 않는 사용자로 수정하면") {
            val command = UpdateUserProfileCommand(
                userId = 999L,
                name = "테스트",
                phone = null,
            )

            every { userRepository.findById(999L) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
