package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.rental.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import java.time.ZonedDateTime

class ReturnRentalUseCaseTest : BehaviorSpec({

    val rentalRepository = mockk<RentalRepository>()
    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = ReturnRentalUseCase(
        rentalRepository = rentalRepository,
        rentalDomainService = rentalDomainService,
    )

    val renterId = 11L

    fun createRental(status: RentalStatus): Rental {
        return Rental(
            rentalId = 1001L,
            renterId = renterId,
            lenderId = 22L,
            productId = 42L,
            status = status,
            startDate = ZonedDateTime.now().minusDays(3),
            endDate = ZonedDateTime.now().plusDays(4),
            totalAmount = 120000L,
            depositAmount = 50000L,
            orderId = "RC-1001-1713063600000",
            deliveryInfo = DeliveryInfo("홍길동", "010-1234-5678", "서울", null, "06234"),
        )
    }

    Given("반납 처리를 요청할 때") {

        When("대여자가 IN_USE 상태의 대여를 반납하면") {
            val rental = createRental(RentalStatus.IN_USE)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateRenterAccess(rental, renterId) }
            every { rentalRepository.save(rental) } returns rental

            val result = useCase.execute(ReturnRentalCommand(rentalId = 1001L, renterId = renterId))

            Then("상태가 RETURNED로 변경된다") {
                result.status shouldBe RentalStatus.RETURNED
            }
        }

        When("IN_USE 상태가 아닌 대여를 반납하려 하면") {
            val rental = createRental(RentalStatus.PAID)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateRenterAccess(rental, renterId) }

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(ReturnRentalCommand(rentalId = 1001L, renterId = renterId))
                }
            }
        }
    }
})
