package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.LenderProfile
import com.rental.commerce.domain.user.LenderProfileRepository
import com.rental.commerce.domain.user.LenderType
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.RenterProfileRepository
import com.rental.commerce.domain.user.TrustGrade
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import com.rental.commerce.domain.user.VerificationStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

class GetMyPageUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val lenderProfileRepository = mockk<LenderProfileRepository>()
    val renterProfileRepository = mockk<RenterProfileRepository>()
    val useCase = GetMyPageUseCase(
        userRepository = userRepository,
        lenderProfileRepository = lenderProfileRepository,
        renterProfileRepository = renterProfileRepository,
    )

    Given("마이페이지 조회 시") {

        When("BOTH 역할 사용자가 조회하면") {
            val userId = 1L
            val user = User(
                email = "test@example.com",
                name = "홍길동",
                phone = "010-1234-5678",
                passwordHash = "hashed",
                role = UserRole.BOTH,
                userId = userId,
            )
            val lenderProfile = LenderProfile(
                lenderProfileId = 1L,
                userId = userId,
                lenderType = LenderType.INDIVIDUAL,
                verificationStatus = VerificationStatus.VERIFIED,
                settlementAccountBank = "신한은행",
                settlementAccountNumber = "110-123-456789",
            )
            val renterProfile = RenterProfile(
                renterProfileId = 1L,
                userId = userId,
                trustGrade = TrustGrade.SILVER,
                totalTransactionCount = 15,
                shippingAddress = "서울시 강남구",
            )

            every { userRepository.findById(userId) } returns user
            every { lenderProfileRepository.findByUserId(userId) } returns lenderProfile
            every { renterProfileRepository.findByUserId(userId) } returns renterProfile

            val result = useCase.execute(userId)

            Then("사용자 기본 정보가 포함된다") {
                result.userId shouldBe userId
                result.email shouldBe "test@example.com"
                result.name shouldBe "홍길동"
                result.phone shouldBe "010-1234-5678"
                result.role shouldBe "BOTH"
            }

            Then("대여자 프로필이 포함된다") {
                result.lenderProfile shouldNotBe null
                result.lenderProfile?.lenderType shouldBe "INDIVIDUAL"
            }

            Then("임차인 프로필이 포함된다") {
                result.renterProfile shouldNotBe null
                result.renterProfile?.trustGrade shouldBe "SILVER"
            }
        }

        When("RENTER 역할 사용자가 조회하면") {
            val userId = 2L
            val user = User(
                email = "renter@example.com",
                name = "김렌터",
                phone = "010-5555-6666",
                passwordHash = "hashed",
                role = UserRole.RENTER,
                userId = userId,
            )
            val renterProfile = RenterProfile(
                renterProfileId = 2L,
                userId = userId,
                trustGrade = TrustGrade.BRONZE,
                totalTransactionCount = 0,
            )

            every { userRepository.findById(userId) } returns user
            every { lenderProfileRepository.findByUserId(userId) } returns null
            every { renterProfileRepository.findByUserId(userId) } returns renterProfile

            val result = useCase.execute(userId)

            Then("대여자 프로필은 null이다") {
                result.lenderProfile shouldBe null
            }

            Then("임차인 프로필이 포함된다") {
                result.renterProfile shouldNotBe null
            }
        }

        When("존재하지 않는 사용자로 조회하면") {
            val userId = 999L

            every { userRepository.findById(userId) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(userId)
                }
            }
        }
    }
})
