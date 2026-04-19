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

class SuspendUserUseCaseTest : BehaviorSpec({

    val adminDomainService = mockk<AdminDomainService>()
    val useCase = SuspendUserUseCase(adminDomainService)

    beforeEach {
        clearMocks(adminDomainService)
    }

    // ─────────────────────────────────────────────────────────────
    // execute() — 사용자 정지
    // ─────────────────────────────────────────────────────────────

    Given("SuspendUserUseCase.execute()") {

        When("유효한 userId 로 정지를 요청하면") {
            Then("adminDomainService.suspendUser()가 1회 호출된다") {
                val userId = 1L
                every { adminDomainService.suspendUser(userId) } just Runs

                useCase.execute(SuspendUserCommand(userId = userId))

                verify(exactly = 1) { adminDomainService.suspendUser(userId) }
            }
        }

        When("존재하지 않는 userId 로 정지를 요청하면") {
            Then("USER_NOT_FOUND 예외가 발생한다") {
                val userId = 999L
                every { adminDomainService.suspendUser(userId) } throws BusinessException(
                    errorCode = ErrorCode.USER_NOT_FOUND,
                    message = "사용자를 찾을 수 없습니다. userId=$userId",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(SuspendUserCommand(userId = userId))
                }
                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }
    }
})
