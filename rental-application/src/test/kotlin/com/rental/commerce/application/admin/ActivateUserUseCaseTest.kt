package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminDomainService
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class ActivateUserUseCaseTest : BehaviorSpec({

    val adminDomainService = mockk<AdminDomainService>()
    val useCase = ActivateUserUseCase(adminDomainService)

    beforeEach {
        clearMocks(adminDomainService)
    }

    // ─────────────────────────────────────────────────────────────
    // execute() — 사용자 활성화
    // ─────────────────────────────────────────────────────────────

    Given("ActivateUserUseCase.execute()") {

        When("유효한 userId 로 활성화를 요청하면") {
            Then("adminDomainService.activateUser()가 1회 호출된다") {
                val userId = 1L
                every { adminDomainService.activateUser(userId) } just Runs

                useCase.execute(ActivateUserCommand(userId = userId))

                verify(exactly = 1) { adminDomainService.activateUser(userId) }
            }
        }

        When("존재하지 않는 userId 로 활성화를 요청하면") {
            Then("USER_NOT_FOUND 예외가 발생한다") {
                val userId = 999L
                every { adminDomainService.activateUser(userId) } throws BusinessException(
                    errorCode = ErrorCode.USER_NOT_FOUND,
                    message = "사용자를 찾을 수 없습니다. userId=$userId",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(ActivateUserCommand(userId = userId))
                }
                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }
    }
})
