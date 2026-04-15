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
import io.mockk.verify

// BLK-002: GET /api/v1/auth/me 엔드포인트 미구현 — UseCase 단위 테스트
class GetMeUseCaseTest : BehaviorSpec({

    Given("현재 로그인한 사용자 정보를 조회할 때") {

        When("유효한 RENTER userId로 조회하면") {
            val userDomainService = mockk<UserDomainService>()
            val useCase = GetMeUseCase(userDomainService)
            val userId = 1L
            val user = User(
                email = "user@example.com",
                name = "홍길동",
                phone = "010-1234-5678",
                passwordHash = "hashed",
                role = UserRole.RENTER,
                id = userId,
            )
            every { userDomainService.findById(userId) } returns user

            Then("사용자 기본 정보가 담긴 MeResponse가 반환된다") {
                val result = useCase.execute(userId)
                result.id shouldBe userId
                result.email shouldBe "user@example.com"
                result.name shouldBe "홍길동"
                result.role shouldBe "RENTER"
            }

            Then("userDomainService.findById가 호출된다") {
                useCase.execute(userId)
                verify(atLeast = 1) { userDomainService.findById(userId) }
            }
        }

        When("BOTH 역할 사용자를 조회하면") {
            val userDomainService = mockk<UserDomainService>()
            val useCase = GetMeUseCase(userDomainService)
            val userId = 2L
            val user = User(
                email = "both@example.com",
                name = "김양쪽",
                phone = "010-9999-8888",
                passwordHash = "hashed",
                role = UserRole.BOTH,
                id = userId,
            )
            every { userDomainService.findById(userId) } returns user

            Then("role이 BOTH로 반환된다") {
                val result = useCase.execute(userId)
                result.role shouldBe "BOTH"
            }
        }

        When("존재하지 않는 userId로 조회하면") {
            val userDomainService = mockk<UserDomainService>()
            val useCase = GetMeUseCase(userDomainService)
            val userId = 999L
            every { userDomainService.findById(userId) } throws ResourceNotFoundException(
                errorCode = ErrorCode.USER_NOT_FOUND,
            )

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(userId)
                }
            }
        }
    }
})
