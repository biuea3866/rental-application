package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class UpdateUserProfileUseCaseTest : BehaviorSpec({

    val userDomainService = mockk<UserDomainService>()
    val useCase = UpdateUserProfileUseCase(userDomainService)

    Given("사용자 기본 정보 수정 시") {

        When("이름과 전화번호를 수정하면") {
            val userId = 1L
            val user = User(
                email = "test@example.com",
                name = "홍길동",
                phone = "010-1234-5678",
                passwordHash = "hashed",
                role = UserRole.RENTER,
                id = userId,
            )
            val command = UpdateUserProfileCommand(
                userId = userId,
                name = "김철수",
                phone = "010-9999-8888",
            )

            every { userDomainService.findById(userId) } returns user

            useCase.execute(command)

            Then("이름이 변경된다") {
                user.name shouldBe "김철수"
            }

            Then("전화번호가 변경된다") {
                user.phone shouldBe "010-9999-8888"
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
                id = userId,
            )
            val command = UpdateUserProfileCommand(
                userId = userId,
                name = "새이름",
                phone = null,
            )

            every { userDomainService.findById(userId) } returns user

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

            every { userDomainService.findById(999L) } throws ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
