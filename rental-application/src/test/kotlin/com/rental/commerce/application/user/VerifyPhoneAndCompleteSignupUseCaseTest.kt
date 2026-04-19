package com.rental.commerce.application.user

import com.rental.commerce.application.auth.RefreshTokenResult
import com.rental.commerce.application.auth.RefreshTokenService
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserDomainService
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

class VerifyPhoneAndCompleteSignupUseCaseTest : BehaviorSpec({

    val userDomainService = mockk<UserDomainService>()
    val phoneVerificationStore = mockk<PhoneVerificationStore>(relaxed = true)
    val tokenProvider = mockk<TokenProvider>()
    val refreshTokenService = mockk<RefreshTokenService>()
    val passwordHasher = mockk<PasswordHasher>()
    val useCase = VerifyPhoneAndCompleteSignupUseCase(
        userDomainService, phoneVerificationStore, tokenProvider, refreshTokenService, passwordHasher,
    )

    Given("휴대폰 인증 완료 시") {

        When("유효한 인증코드로 요청하면") {
            val command = VerifyPhoneCommand(
                email = "test@example.com",
                phone = "01012345678",
                code = "123456",
                password = "password123!",
                name = "홍길동",
                role = UserRole.RENTER,
            )

            every { phoneVerificationStore.verify(command.phone, command.code) } returns "test@example.com"
            every { userDomainService.checkEmailNotDuplicate(command.email) } just runs
            every { passwordHasher.hash(command.password) } returns "bcrypt_hashed"

            val savedUserSlot = slot<User>()
            every { userDomainService.saveUser(capture(savedUserSlot)) } answers {
                User(
                    email = savedUserSlot.captured.email,
                    name = savedUserSlot.captured.name,
                    phone = savedUserSlot.captured.phone,
                    passwordHash = savedUserSlot.captured.passwordHash,
                    role = savedUserSlot.captured.role,
                    id = 1L,
                )
            }

            every { tokenProvider.createAccessToken(1L, "RENTER") } returns "access_token_123"
            every { refreshTokenService.issueRefreshToken(1L) } returns RefreshTokenResult(
                refreshToken = "refresh_token_123",
                tokenFamily = "family_123",
                userId = 1L,
            )
            every { phoneVerificationStore.delete(command.phone) } returns Unit

            val result = useCase.execute(command)

            Then("User가 DomainService를 통해 저장된다") {
                verify(exactly = 1) { userDomainService.saveUser(any()) }
            }

            Then("Access Token이 발급된다") {
                result.accessToken shouldBe "access_token_123"
            }

            Then("Refresh Token이 발급된다") {
                result.refreshToken shouldBe "refresh_token_123"
            }

            Then("userId가 반환된다") {
                result.userId shouldBe 1L
            }

            Then("인증코드가 삭제된다") {
                verify(exactly = 1) { phoneVerificationStore.delete(command.phone) }
            }
        }

        When("인증코드가 일치하지 않으면") {
            val command = VerifyPhoneCommand(
                email = "test@example.com",
                phone = "01012345678",
                code = "999999",
                password = "password123!",
                name = "홍길동",
                role = UserRole.RENTER,
            )

            every { phoneVerificationStore.verify(command.phone, command.code) } throws
                BusinessException(ErrorCode.INVALID_VERIFICATION_CODE)

            Then("INVALID_VERIFICATION_CODE 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_VERIFICATION_CODE
            }
        }

        When("인증코드가 만료되었으면") {
            val command = VerifyPhoneCommand(
                email = "test@example.com",
                phone = "01012345678",
                code = "123456",
                password = "password123!",
                name = "홍길동",
                role = UserRole.RENTER,
            )

            every { phoneVerificationStore.verify(command.phone, command.code) } throws
                BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED)

            Then("VERIFICATION_CODE_EXPIRED 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.VERIFICATION_CODE_EXPIRED
            }
        }

        When("인증 요청 이메일과 가입 이메일이 다르면") {
            val command = VerifyPhoneCommand(
                email = "different@example.com",
                phone = "01012345678",
                code = "123456",
                password = "password123!",
                name = "홍길동",
                role = UserRole.RENTER,
            )

            every { phoneVerificationStore.verify(command.phone, command.code) } returns "test@example.com"

            Then("INVALID_INPUT 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("이미 가입된 이메일이면") {
            val command = VerifyPhoneCommand(
                email = "existing@example.com",
                phone = "01012345678",
                code = "123456",
                password = "password123!",
                name = "홍길동",
                role = UserRole.RENTER,
            )

            every { phoneVerificationStore.verify(command.phone, command.code) } returns "existing@example.com"
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
