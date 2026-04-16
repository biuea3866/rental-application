package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
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
import io.mockk.verify
import java.time.ZonedDateTime

class ApproveRentalUseCaseTest : BehaviorSpec({

    val rentalRepository = mockk<RentalRepository>()
    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = ApproveRentalUseCase(
        rentalRepository = rentalRepository,
        rentalDomainService = rentalDomainService,
    )

    val lenderId = 22L
    val renterId = 11L

    fun createRental(status: RentalStatus = RentalStatus.REQUESTED): Rental {
        return Rental(
            rentalId = 1001L,
            renterId = renterId,
            lenderId = lenderId,
            productId = 42L,
            status = status,
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(7),
            totalAmount = 70000L,
            depositAmount = 50000L,
            orderId = "RC-1001-1234567890",
            deliveryInfo = DeliveryInfo(
                recipientName = "홍길동",
                recipientPhone = "010-1234-5678",
                addressLine1 = "서울특별시 강남구",
                addressLine2 = null,
                zipCode = "06234",
            ),
        )
    }

    Given("대여 승인을 요청할 때") {

        When("등록자가 REQUESTED 상태의 대여를 승인하면") {
            val rental = createRental(RentalStatus.REQUESTED)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateLenderAccess(rental, lenderId) }
            every { rentalRepository.save(rental) } returns rental

            val result = useCase.execute(ApproveRentalCommand(rentalId = 1001L, lenderId = lenderId))

            Then("대여 상태가 APPROVED로 변경된다") {
                result.status shouldBe RentalStatus.APPROVED
            }

            Then("rentalRepository.save가 호출된다") {
                verify(exactly = 1) { rentalRepository.save(rental) }
            }
        }

        When("등록자가 아닌 사용자가 승인을 시도하면") {
            val rental = createRental(RentalStatus.REQUESTED)
            val otherUserId = 99L

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            every {
                rentalDomainService.validateLenderAccess(rental, otherUserId)
            } throws BusinessException(ErrorCode.RENTAL_LENDER_ONLY)

            Then("RENTAL_LENDER_ONLY 예외가 발생한다") {
                shouldThrow<BusinessException> {
                    useCase.execute(ApproveRentalCommand(rentalId = 1001L, lenderId = otherUserId))
                }.errorCode shouldBe ErrorCode.RENTAL_LENDER_ONLY
            }
        }

        When("REQUESTED 상태가 아닌 대여를 승인하려 하면") {
            val rental = createRental(RentalStatus.APPROVED)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateLenderAccess(rental, lenderId) }

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(ApproveRentalCommand(rentalId = 1001L, lenderId = lenderId))
                }
            }
        }
    }
})
