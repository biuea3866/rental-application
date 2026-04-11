package com.rental.commerce.application.user

import com.rental.commerce.application.auth.RefreshTokenResult
import com.rental.commerce.application.auth.RefreshTokenService
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PhoneVerificationStore
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.TokenProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class VerifyPhoneAndCompleteSignupUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val phoneVerificationStore = mockk<PhoneVerificationStore>(relaxed = true)
    val tokenProvider = mockk<TokenProvider>()
    val refreshTokenService = mockk<RefreshTokenService>()
    val passwordHasher = mockk<PasswordHasher>()
    val useCase = VerifyPhoneAndCompleteSignupUseCase(
        userRepository, phoneVerificationStore, tokenProvider, refreshTokenService, passwordHasher,
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

            every { phoneVerificationStore.findByPhone(command.phone) } returns "123456"
            every { passwordHasher.hash(command.password) } returns "bcrypt_hashed"
            every { userRepository.existsByEmail(command.email) } returns false

            val savedUserSlot = slot<User>()
            every { userRepository.save(capture(savedUserSlot)) } answers {
                savedUserSlot.captured.apply {
                    // userId는 DB 자동생성이므로 reflection으로 설정할 수 없음 — mock에서 고정값 반환
                }
                User(
                    email = savedUserSlot.captured.email,
                    name = savedUserSlot.captured.name,
                    phone = savedUserSlot.captured.phone,
                    passwordHash = savedUserSlot.captured.passwordHash,
                    role = savedUserSlot.captured.role,
                    userId = 1L,
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

            Then("User가 DB에 저장된다") {
                verify(exactly = 1) { userRepository.save(any()) }
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

            every { phoneVerificationStore.findByPhone(command.phone) } returns "123456"

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

            every { phoneVerificationStore.findByPhone(command.phone) } returns null

            Then("VERIFICATION_CODE_EXPIRED 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.VERIFICATION_CODE_EXPIRED
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

            every { phoneVerificationStore.findByPhone(command.phone) } returns "123456"
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
