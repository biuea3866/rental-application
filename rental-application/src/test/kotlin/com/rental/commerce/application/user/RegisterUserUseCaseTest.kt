package com.rental.commerce.application.user

import com.rental.commerce.domain.auth.AuthDomainService
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify

class RegisterUserUseCaseTest : BehaviorSpec({

    val userDomainService = mockk<UserDomainService>()
    val phoneVerificationStore = mockk<PhoneVerificationStore>(relaxed = true)
    val authDomainService = mockk<AuthDomainService>()
    val useCase = RegisterUserUseCase(userDomainService, phoneVerificationStore, authDomainService)

    Given("회원가입 요청 시") {

        When("유효한 정보로 가입 요청하면") {
            val command = RegisterUserCommand(
                email = "test@example.com",
                password = "password123!",
                name = "홍길동",
                phone = "01012345678",
                role = UserRole.RENTER,
            )

            every { userDomainService.checkEmailNotDuplicate(command.email) } just runs
            every { authDomainService.sendPhoneVerificationCode(command.phone) } returns "123456"

            val codeSlot = slot<String>()
            every { phoneVerificationStore.save(command.phone, capture(codeSlot), command.email, any()) } returns Unit

            val result = useCase.execute(command)

            Then("인증코드가 6자리로 생성되어 저장된다") {
                codeSlot.captured shouldHaveLength 6
            }

            Then("PhoneVerificationStore에 이메일과 함께 저장된다") {
                verify(exactly = 1) { phoneVerificationStore.save(command.phone, any(), command.email, any()) }
            }

            Then("AuthDomainService를 통해 인증코드가 발송된다") {
                verify(exactly = 1) { authDomainService.sendPhoneVerificationCode(command.phone) }
            }

            Then("응답에 이메일과 전화번호가 포함된다") {
                result.email shouldBe command.email
                result.phone shouldBe command.phone
            }
        }

        When("이미 가입된 이메일로 요청하면") {
            val command = RegisterUserCommand(
                email = "existing@example.com",
                password = "password123!",
                name = "홍길동",
                phone = "01012345678",
                role = UserRole.RENTER,
            )

            every { userDomainService.checkEmailNotDuplicate(command.email) } throws
                DuplicateResourceException(
                    errorCode = ErrorCode.DUPLICATE_EMAIL,
                )

            Then("DUPLICATE_EMAIL 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.DUPLICATE_EMAIL
            }
        }
    }
})
