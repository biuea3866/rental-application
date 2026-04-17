package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class ReturnRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = ReturnRentalUseCase(rentalDomainService)

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    val renterId = 10L
    val lenderId = 20L

    fun createInUseRental(rentalId: Long = 1L): Rental {
        val rental = Rental.create(
            renterId = renterId,
            lenderId = lenderId,
            productId = 42L,
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(8),
            totalAmount = 70_000L,
            depositAmount = 50_000L,
            deliveryInfo = deliveryInfo,
        )
        rental.approve()
        rental.markPaid()
        rental.startRental()
        return rental
    }

    beforeEach {
        clearMocks(rentalDomainService)
    }

    Given("대여 반납 요청을 할 때") {

        When("대여자(renter)가 IN_USE 상태의 대여를 반납하면") {
            Then("RentalDomainService.returnRental()이 호출되고 RETURNED 상태로 전이된다") {
                val rental = createInUseRental(rentalId = 1L)

                val command = ReturnRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.returnRental(rental) } answers {
                    rental.returnRental()
                    rental
                }

                useCase.execute(command)

                rental.status shouldBe RentalStatus.RETURNED
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalDomainService.returnRental(rental) }
            }
        }

        When("등록자(lender)가 IN_USE 상태의 대여를 반납 처리하면") {
            Then("RentalDomainService.returnRental()이 호출되고 RETURNED 상태로 저장된다 (양측 모두 가능)") {
                val rental = createInUseRental(rentalId = 1L)

                val command = ReturnRentalCommand(
                    rentalId = 1L,
                    userId = lenderId,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.returnRental(rental) } answers {
                    rental.returnRental()
                    rental
                }

                useCase.execute(command)

                rental.status shouldBe RentalStatus.RETURNED
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalDomainService.returnRental(rental) }
            }
        }

        When("IN_USE가 아닌 상태(PAID)의 대여를 반납하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
                val paidRental = Rental.create(
                    renterId = renterId,
                    lenderId = lenderId,
                    productId = 42L,
                    startDate = ZonedDateTime.now().plusDays(1),
                    endDate = ZonedDateTime.now().plusDays(8),
                    totalAmount = 70_000L,
                    depositAmount = 50_000L,
                    deliveryInfo = deliveryInfo,
                )
                paidRental.approve()
                paidRental.markPaid()

                val command = ReturnRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                )

                every { rentalDomainService.getRentalById(1L) } returns paidRental
                every { rentalDomainService.returnRental(paidRental) } throws InvalidStateTransitionException(
                    "PAID에서 RETURNED(으)로 전이할 수 없습니다"
                )

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }

        When("존재하지 않는 대여를 반납하려고 하면") {
            Then("RentalNotFoundException 예외가 발생한다") {
                val nonExistentRentalId = 9999L
                val command = ReturnRentalCommand(
                    rentalId = nonExistentRentalId,
                    userId = renterId,
                )

                every { rentalDomainService.getRentalById(nonExistentRentalId) } throws RentalNotFoundException(
                    "대여를 찾을 수 없습니다. rentalId=$nonExistentRentalId"
                )

                shouldThrow<RentalNotFoundException> {
                    useCase.execute(command)
                }
                verify(exactly = 0) { rentalDomainService.returnRental(any()) }
            }
        }

        When("RETURNED 상태의 대여를 다시 반납하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
                val rental = createInUseRental()
                rental.returnRental()

                val command = ReturnRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.returnRental(rental) } throws InvalidStateTransitionException(
                    "RETURNED에서 RETURNED(으)로 전이할 수 없습니다"
                )

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
