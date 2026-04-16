package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentCancelResult
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
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

class CancelRentalUseCaseTest : BehaviorSpec({

    val rentalRepository = mockk<RentalRepository>()
    val rentalPaymentRepository = mockk<RentalPaymentRepository>()
    val paymentGateway = mockk<PaymentGateway>()
    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = CancelRentalUseCase(
        rentalRepository = rentalRepository,
        rentalPaymentRepository = rentalPaymentRepository,
        paymentGateway = paymentGateway,
        rentalDomainService = rentalDomainService,
    )

    val renterId = 11L
    val lenderId = 22L

    fun createRental(status: RentalStatus): Rental {
        return Rental(
            rentalId = 1001L,
            renterId = renterId,
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

    Given("대여 취소를 요청할 때") {

        When("대여자가 REQUESTED 상태의 대여를 취소하면") {
            val rental = createRental(RentalStatus.REQUESTED)
            val reason = "일정이 변경되었습니다."

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateParticipantAccess(rental, renterId) }
            every { rentalPaymentRepository.findByRentalId(1001L) } returns null
            every { rentalRepository.save(rental) } returns rental

            val result = useCase.execute(CancelRentalCommand(rentalId = 1001L, userId = renterId, reason = reason))

            Then("상태가 CANCELLED로 변경된다") {
                result.status shouldBe RentalStatus.CANCELLED
            }

            Then("cancelReason이 설정된다") {
                result.cancelReason shouldBe reason
            }
        }

        When("APPROVED 상태의 대여를 취소하면 결제가 없으므로 환불 없이 취소된다") {
            val rental = createRental(RentalStatus.APPROVED)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateParticipantAccess(rental, renterId) }
            every { rentalPaymentRepository.findByRentalId(1001L) } returns null
            every { rentalRepository.save(rental) } returns rental

            val result = useCase.execute(CancelRentalCommand(rentalId = 1001L, userId = renterId, reason = "사유"))

            Then("상태가 CANCELLED로 변경된다") {
                result.status shouldBe RentalStatus.CANCELLED
            }

            Then("paymentGateway.cancel이 호출되지 않는다") {
                verify(exactly = 0) { paymentGateway.cancel(any(), any(), any()) }
            }
        }

        When("PAID 상태의 대여를 취소하면") {
            val rental = createRental(RentalStatus.PAID)
            val existingPayment = RentalPayment(
                paymentId = 5001L,
                rentalId = 1001L,
                amount = 120000L,
                paymentMethod = PaymentMethod.CARD,
                status = PaymentStatus.COMPLETED,
                externalPaymentId = "tossKey123",
                orderId = "RC-1001-1713063600000",
                paidAt = ZonedDateTime.now().minusHours(1),
            )

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateParticipantAccess(rental, renterId) }
            every { rentalPaymentRepository.findByRentalId(1001L) } returns existingPayment
            every {
                paymentGateway.cancel("tossKey123", 120000L, any())
            } returns PaymentCancelResult(
                paymentKey = "tossKey123",
                cancelledAt = ZonedDateTime.now(),
                cancelAmount = 120000L,
            )
            every { rentalPaymentRepository.save(any()) } answers { firstArg() }
            every { rentalRepository.save(rental) } returns rental

            val result = useCase.execute(CancelRentalCommand(rentalId = 1001L, userId = renterId, reason = "사유"))

            Then("상태가 CANCELLED로 변경된다") {
                result.status shouldBe RentalStatus.CANCELLED
            }

            Then("paymentGateway.cancel이 호출된다") {
                verify(exactly = 1) { paymentGateway.cancel("tossKey123", 120000L, any()) }
            }
        }

        When("당사자가 아닌 사용자가 취소를 시도하면") {
            val rental = createRental(RentalStatus.REQUESTED)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            every {
                rentalDomainService.validateParticipantAccess(rental, 99L)
            } throws BusinessException(ErrorCode.RENTAL_ACCESS_DENIED)

            Then("RENTAL_ACCESS_DENIED 예외가 발생한다") {
                shouldThrow<BusinessException> {
                    useCase.execute(CancelRentalCommand(rentalId = 1001L, userId = 99L, reason = "사유"))
                }.errorCode shouldBe ErrorCode.RENTAL_ACCESS_DENIED
            }
        }

        When("IN_USE 상태의 대여를 취소하려 하면") {
            val rental = createRental(RentalStatus.IN_USE)

            every { rentalDomainService.getOrThrow(1001L) } returns rental
            justRun { rentalDomainService.validateParticipantAccess(rental, renterId) }
            every { rentalPaymentRepository.findByRentalId(1001L) } returns null

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(CancelRentalCommand(rentalId = 1001L, userId = renterId, reason = "사유"))
                }
            }
        }
    }
})
