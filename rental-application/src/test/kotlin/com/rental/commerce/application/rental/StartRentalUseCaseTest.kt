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

class StartRentalUseCaseTest : BehaviorSpec({

    val rentalRepository = mockk<RentalRepository>()
    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = StartRentalUseCase(
        rentalRepository = rentalRepository,
        rentalDomainService = rentalDomainService,
    )

    val lenderId = 22L

    fun createRental(status: RentalStatus): Rental {
        return Rental(
            rentalId = 1001L,
            renterId = 11L,
            lenderId = lenderId,
            productId = 42L,
            status = status,
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(7),
            totalAmount = 120000L,
            depositAmount = 50000L,
            orderId = "RC-1001-1713063600000",
            deliveryInfo = DeliveryInfo("홍길동", "010-1234-5678", "서울", null, "06234"),
        )
    }

    Given("대여 시작을 요청할 때") {

        When("등록자가 PAID 상태의 대여를 시작하면") {
            val rental = createRental(RentalStatus.PAID)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateLenderAccess(rental, lenderId) }
            every { rentalRepository.save(rental) } returns rental

            val result = useCase.execute(StartRentalCommand(rentalId = 1001L, lenderId = lenderId))

            Then("상태가 IN_USE로 변경된다") {
                result.status shouldBe RentalStatus.IN_USE
            }
        }

        When("PAID 상태가 아닌 대여를 시작하려 하면") {
            val rental = createRental(RentalStatus.APPROVED)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateLenderAccess(rental, lenderId) }

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(StartRentalCommand(rentalId = 1001L, lenderId = lenderId))
                }
            }
        }
    }
})
