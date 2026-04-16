package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.rental.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZonedDateTime

class RequestRentalUseCaseTest : BehaviorSpec({

    val rentalRepository = mockk<RentalRepository>()
    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = RequestRentalUseCase(
        rentalRepository = rentalRepository,
        rentalDomainService = rentalDomainService,
    )

    val startDate = ZonedDateTime.now().plusDays(1)
    val endDate = ZonedDateTime.now().plusDays(7)
    val deliveryInfoCommand = DeliveryInfoCommand(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    Given("대여 신청을 요청할 때") {

        When("정상적인 요청이면") {
            val command = RequestRentalCommand(
                renterId = 11L,
                productId = 42L,
                lenderId = 22L,
                startDate = startDate,
                endDate = endDate,
                totalAmount = 70000L,
                depositAmount = 50000L,
                deliveryInfo = deliveryInfoCommand,
            )

            val rentalSlot = slot<Rental>()
            justRun { rentalDomainService.validatePeriodAvailability(any(), any(), any()) }
            every { rentalRepository.save(capture(rentalSlot)) } answers {
                rentalSlot.captured
            }

            val result = useCase.execute(command)

            Then("REQUESTED 상태의 대여가 생성된다") {
                result.status shouldBe RentalStatus.REQUESTED
            }

            Then("renterId가 설정된다") {
                result.renterId shouldBe 11L
            }

            Then("productId가 설정된다") {
                result.productId shouldBe 42L
            }

            Then("orderId가 생성된다") {
                result.orderId shouldNotBe null
                result.orderId.startsWith("RC-") shouldBe true
            }

            Then("RentalDomainService.validatePeriodAvailability가 호출된다") {
                verify(exactly = 1) { rentalDomainService.validatePeriodAvailability(42L, startDate, endDate) }
            }

            Then("RentalRepository.save가 정확히 한 번 호출된다") {
                verify(exactly = 1) { rentalRepository.save(any()) }
            }
        }

        When("기간 충돌이 있으면") {
            val command = RequestRentalCommand(
                renterId = 11L,
                productId = 42L,
                lenderId = 22L,
                startDate = startDate,
                endDate = endDate,
                totalAmount = 70000L,
                depositAmount = 50000L,
                deliveryInfo = deliveryInfoCommand,
            )

            every {
                rentalDomainService.validatePeriodAvailability(any(), any(), any())
            } throws BusinessException(
                errorCode = ErrorCode.RENTAL_PERIOD_CONFLICT,
            )

            Then("RENTAL_PERIOD_CONFLICT 예외가 발생한다") {
                shouldThrow<BusinessException> {
                    useCase.execute(command)
                }.errorCode shouldBe ErrorCode.RENTAL_PERIOD_CONFLICT
            }
        }
    }
})
