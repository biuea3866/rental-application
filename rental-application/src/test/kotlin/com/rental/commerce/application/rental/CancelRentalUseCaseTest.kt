package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentCancelResult
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.rental.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.ZonedDateTime

class CancelRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val rentalRepository = mockk<RentalRepository>()
    val rentalPaymentRepository = mockk<RentalPaymentRepository>()
    val paymentGateway = mockk<PaymentGateway>()
    val rentalEventPublisher = mockk<RentalEventPublisher>()

    val useCase = CancelRentalUseCase(
        rentalDomainService = rentalDomainService,
        rentalRepository = rentalRepository,
        rentalPaymentRepository = rentalPaymentRepository,
        paymentGateway = paymentGateway,
        rentalEventPublisher = rentalEventPublisher,
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
        clearMocks(rentalDomainService, rentalRepository, rentalPaymentRepository, paymentGateway, rentalEventPublisher)
    }

    Given("대여 취소 요청을 할 때") {

        When("REQUESTED 상태의 대여를 대여자가 취소하면") {
            Then("rental.cancel()이 호출되고 CANCELLED 상태로 저장된다") {
                val rental = createRequestedRental()

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalRepository.save(rental) } returns rental
                every { rentalEventPublisher.publishAll(any()) } just runs

                useCase.execute(command)

                rental.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalRepository.save(rental) }
                verify(exactly = 0) { paymentGateway.cancelPayment(any(), any()) }
            }
        }

        When("APPROVED 상태의 대여를 등록자가 취소하면") {
            Then("rental.cancel()이 호출되고 CANCELLED 상태로 저장된다") {
                val rental = createRequestedRental()
                rental.approve()

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = lenderId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalRepository.save(rental) } returns rental
                every { rentalEventPublisher.publishAll(any()) } just runs

                useCase.execute(command)

                rental.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalRepository.save(rental) }
                verify(exactly = 0) { paymentGateway.cancelPayment(any(), any()) }
            }
        }

        When("PAID 상태의 대여를 취소하면") {
            Then("PaymentGateway.cancelPayment()가 호출되고 RentalPayment.refund()가 수행되며 CANCELLED로 저장된다") {
                val rental = createRequestedRental()
                rental.approve()
                rental.markPaid()

                val externalPaymentId = "pay_external_key_123"
                val completedPayment = RentalPayment.create(
                    rentalId = 1L,
                    amount = 70_000L,
                    paymentMethod = PaymentMethod.CARD,
                    orderId = "RC-1-1713063600000",
                ).also { it.complete(externalPaymentId) }

                val cancelResult = PaymentCancelResult(
                    success = true,
                    cancelAmount = 70_000L,
                    canceledAt = ZonedDateTime.now(),
                )

                val command = CancelRentalCommand(
                    rentalId = 1L,
                    userId = renterId,
                    reason = cancelReason,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalPaymentRepository.findByRentalId(1L) } returns completedPayment
                every { paymentGateway.cancelPayment(externalPaymentId, cancelReason) } returns cancelResult
                every { rentalPaymentRepository.save(completedPayment) } returns completedPayment
                every { rentalRepository.save(rental) } returns rental
                every { rentalEventPublisher.publishAll(any()) } just runs

                useCase.execute(command)

                rental.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 1) { paymentGateway.cancelPayment(externalPaymentId, cancelReason) }
                verify(exactly = 1) { rentalPaymentRepository.save(completedPayment) }
                verify(exactly = 1) { rentalRepository.save(rental) }
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

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
                verify(exactly = 0) { rentalRepository.save(any()) }
                verify(exactly = 0) { paymentGateway.cancelPayment(any(), any()) }
            }
        }
    }
})
