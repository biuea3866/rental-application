package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.RenterProfileRepository
import com.rental.commerce.domain.user.TrustGrade
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class UpdateRenterProfileUseCaseTest : BehaviorSpec({

    val renterProfileRepository = mockk<RenterProfileRepository>()
    val useCase = UpdateRenterProfileUseCase(renterProfileRepository)

    Given("임차인 프로필 수정 시") {

        When("배송지 주소를 수정하면") {
            val userId = 1L
            val profile = RenterProfile(
                renterProfileId = 1L,
                userId = userId,
                trustGrade = TrustGrade.SILVER,
                totalTransactionCount = 15,
                shippingAddress = "서울시 강남구",
            )
            val command = UpdateRenterProfileCommand(
                userId = userId,
                shippingAddress = "서울시 서초구",
            )

            every { renterProfileRepository.findByUserId(userId) } returns profile

            val result = useCase.execute(command)

            Then("배송지 주소가 변경된다") {
                result.shippingAddress shouldBe "서울시 서초구"
            }
        }

        When("null을 전달하면") {
            val userId = 2L
            val profile = RenterProfile(
                renterProfileId = 2L,
                userId = userId,
                trustGrade = TrustGrade.BRONZE,
                totalTransactionCount = 0,
                shippingAddress = "서울시 강남구",
            )
            val command = UpdateRenterProfileCommand(
                userId = userId,
                shippingAddress = null,
            )

            every { renterProfileRepository.findByUserId(userId) } returns profile

            val result = useCase.execute(command)

            Then("배송지 주소가 유지된다") {
                result.shippingAddress shouldBe "서울시 강남구"
            }
        }

        When("프로필이 존재하지 않으면") {
            val command = UpdateRenterProfileCommand(
                userId = 999L,
                shippingAddress = "서울시 서초구",
            )

            every { renterProfileRepository.findByUserId(999L) } returns null

            Then("ResourceNotFoundException이 발생한다") {
                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
