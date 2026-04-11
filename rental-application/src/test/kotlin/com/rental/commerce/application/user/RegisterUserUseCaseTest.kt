package com.rental.commerce.application.user

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.common.SmsGateway
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class RegisterUserUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val phoneVerificationStore = mockk<PhoneVerificationStore>(relaxed = true)
    val smsGateway = mockk<SmsGateway>(relaxed = true)
    val useCase = RegisterUserUseCase(userRepository, phoneVerificationStore, smsGateway)

    Given("회원가입 요청 시") {

        When("유효한 정보로 가입 요청하면") {
            val command = RegisterUserCommand(
                email = "test@example.com",
                password = "password123!",
                name = "홍길동",
                phone = "01012345678",
                role = UserRole.RENTER,
            )

            every { userRepository.existsByEmail(command.email) } returns false

            val codeSlot = slot<String>()
            every { phoneVerificationStore.save(command.phone, capture(codeSlot), any()) } returns Unit

            val result = useCase.execute(command)

            Then("인증코드가 6자리로 생성되어 발송된다") {
                codeSlot.captured shouldHaveLength 6
            }

            Then("SMS 게이트웨이로 인증코드가 발송된다") {
                verify(exactly = 1) { smsGateway.sendVerificationCode(command.phone, any()) }
            }

            Then("인증코드가 PhoneVerificationStore에 저장된다") {
                verify(exactly = 1) { phoneVerificationStore.save(command.phone, any(), any()) }
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

            every { userRepository.existsByEmail(command.email) } returns true

            Then("DUPLICATE_EMAIL 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.DUPLICATE_EMAIL
            }
        }
    }
})
