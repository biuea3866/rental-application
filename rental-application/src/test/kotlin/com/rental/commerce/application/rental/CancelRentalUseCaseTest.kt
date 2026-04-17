package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.InvalidStateTransitionException
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

class CancelRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()

    val useCase = CancelRentalUseCase(
        rentalDomainService = rentalDomainService,
    )

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    val renterId = 10L
    val lenderId = 20L
    val cancelReason = "단순 변심"

    fun createRequestedRental(): Rental =
        Rental.create(
            renterId = renterId,
            lenderId = lenderId,
            productId = 42L,
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(8),
            totalAmount = 70_000L,
            depositAmount = 50_000L,
            deliveryInfo = deliveryInfo,
        )

    beforeEach {
        clearMocks(rentalDomainService)
    }

    Given("대여 취소 요청을 할 때") {

        When("REQUESTED 상태의 대여를 대여자가 취소하면") {
            Then("RentalDomainService.cancelRental()이 호출되고 CANCELLED 상태로 전이된다") {
                val rental = createRequestedRental()

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.cancelRental(rental, cancelReason) } answers {
                    rental.cancel(cancelReason)
                    rental
                }

                useCase.execute(command)

                rental.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalDomainService.cancelRental(rental, cancelReason) }
            }
        }

        When("APPROVED 상태의 대여를 등록자가 취소하면") {
            Then("RentalDomainService.cancelRental()이 호출되고 CANCELLED 상태로 전이된다") {
                val rental = createRequestedRental()
                rental.approve()

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = lenderId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.cancelRental(rental, cancelReason) } answers {
                    rental.cancel(cancelReason)
                    rental
                }

                useCase.execute(command)

                rental.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalDomainService.cancelRental(rental, cancelReason) }
            }
        }

        When("PAID 상태의 대여를 취소하면") {
            Then("RentalDomainService.cancelRental()이 호출되고 환불 처리 후 CANCELLED로 전이된다") {
                val rental = createRequestedRental()
                rental.approve()
                rental.markPaid()

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.cancelRental(rental, cancelReason) } answers {
                    rental.cancel(cancelReason)
                    rental
                }

                useCase.execute(command)

                rental.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalDomainService.cancelRental(rental, cancelReason) }
            }
        }

        When("IN_USE 상태의 대여를 취소하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
                val rental = createRequestedRental()
                rental.approve()
                rental.markPaid()
                rental.startRental()

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.cancelRental(rental, cancelReason) } throws InvalidStateTransitionException(
                    "IN_USE에서 CANCELLED(으)로 전이할 수 없습니다"
                )

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
                verify(exactly = 1) { rentalDomainService.cancelRental(rental, cancelReason) }
            }
        }

        When("RETURNED 상태의 대여를 취소하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
                val rental = createRequestedRental()
                rental.approve()
                rental.markPaid()
                rental.startRental()
                rental.returnRental()

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.cancelRental(rental, cancelReason) } throws InvalidStateTransitionException(
                    "RETURNED에서 CANCELLED(으)로 전이할 수 없습니다"
                )

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
                verify(exactly = 1) { rentalDomainService.cancelRental(rental, cancelReason) }
            }
        }

        When("이미 CANCELLED 상태의 대여를 다시 취소하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
                val rental = createRequestedRental()
                rental.cancel(cancelReason)

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                    reason = "중복 취소 시도",
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalDomainService.cancelRental(rental, "중복 취소 시도") } throws InvalidStateTransitionException(
                    "CANCELLED에서 CANCELLED(으)로 전이할 수 없습니다"
                )

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
