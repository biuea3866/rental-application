package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentApproveResult
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
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class ProcessPaymentUseCaseTest : DescribeSpec({

    val rentalRepository = mockk<RentalRepository>()
    val rentalPaymentRepository = mockk<RentalPaymentRepository>()
    val paymentGateway = mockk<PaymentGateway>()
    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = ProcessPaymentUseCase(
        rentalRepository = rentalRepository,
        rentalPaymentRepository = rentalPaymentRepository,
        paymentGateway = paymentGateway,
        rentalDomainService = rentalDomainService,
    )

    val lenderId = 22L
    val renterId = 11L
    val rentalId = 1001L

    beforeEach { clearAllMocks() }

    fun createApprovedRental(): Rental {
        return Rental(
            rentalId = rentalId,
            renterId = renterId,
            lenderId = lenderId,
            productId = 42L,
            status = RentalStatus.APPROVED,
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(7),
            totalAmount = 120000L,
            depositAmount = 50000L,
            orderId = "RC-1001-1713063600000",
            deliveryInfo = DeliveryInfo(
                recipientName = "홍길동",
                recipientPhone = "010-1234-5678",
                addressLine1 = "서울특별시 강남구",
                addressLine2 = null,
                zipCode = "06234",
            ),
        )
    }

    describe("APPROVED 상태의 대여에 정상 결제를 요청하면") {

        it("결제가 완료되고 대여 상태가 PAID로 변경된다") {
            val rental = createApprovedRental()
            val command = ProcessPaymentCommand(
                rentalId = rentalId,
                renterId = renterId,
                paymentKey = "5zJ4xY7m0kODnyRpQWGrN2eqXgarbMe7zEX5sGR0",
                orderId = "RC-1001-1713063600000",
                amount = 120000L,
                paymentMethod = PaymentMethod.CARD,
            )

            every { rentalDomainService.getOrThrow(rentalId) } returns rental
            every { rentalPaymentRepository.findByRentalId(rentalId) } returns null
            every { paymentGateway.approve(any()) } returns PaymentApproveResult(
                externalPaymentId = command.paymentKey,
                approvedAt = ZonedDateTime.now(),
                method = "CARD",
            )
            every { rentalRepository.save(rental) } returns rental
            every { rentalPaymentRepository.save(any()) } answers { firstArg() }

            val result = useCase.execute(command)

            result.rentalStatus shouldBe RentalStatus.PAID
            result.paymentStatus shouldBe PaymentStatus.COMPLETED
            verify(exactly = 1) { rentalPaymentRepository.save(any()) }
        }
    }

    describe("이미 결제 완료된 대여에 재요청하면 (멱등성)") {

        it("기존 결제 정보를 반환하고 paymentGateway.approve가 호출되지 않는다") {
            val rental = createApprovedRental().also { it.markPaid() }
            val existingPayment = RentalPayment(
                paymentId = 5001L,
                rentalId = rentalId,
                amount = 120000L,
                paymentMethod = PaymentMethod.CARD,
                status = PaymentStatus.COMPLETED,
                externalPaymentId = "5zJ4xY7m0kODnyRpQWGrN2eqXgarbMe7zEX5sGR0",
                orderId = "RC-1001-1713063600000",
                paidAt = ZonedDateTime.now(),
            )

            every { rentalDomainService.getOrThrow(rentalId) } returns rental
            every { rentalPaymentRepository.findByRentalId(rentalId) } returns existingPayment

            val command = ProcessPaymentCommand(
                rentalId = rentalId,
                renterId = renterId,
                paymentKey = "5zJ4xY7m0kODnyRpQWGrN2eqXgarbMe7zEX5sGR0",
                orderId = "RC-1001-1713063600000",
                amount = 120000L,
                paymentMethod = PaymentMethod.CARD,
            )

            val result = useCase.execute(command)

            result.paymentStatus shouldBe PaymentStatus.COMPLETED
            verify(exactly = 0) { paymentGateway.approve(any()) }
        }
    }

    describe("금액이 불일치하면") {

        it("AMOUNT_MISMATCH 예외가 발생한다") {
            val rental = createApprovedRental()

            every { rentalDomainService.getOrThrow(rentalId) } returns rental
            every { rentalPaymentRepository.findByRentalId(rentalId) } returns null

            val command = ProcessPaymentCommand(
                rentalId = rentalId,
                renterId = renterId,
                paymentKey = "someKey",
                orderId = "RC-1001-1713063600000",
                amount = 99999L,
                paymentMethod = PaymentMethod.CARD,
            )

            shouldThrow<BusinessException> {
                useCase.execute(command)
            }.errorCode shouldBe ErrorCode.AMOUNT_MISMATCH
        }
    }

    describe("결제 게이트웨이가 실패를 반환하면") {

        it("PaymentFailedException이 발생하고 FAILED 결제 레코드가 저장된다") {
            val rental = createApprovedRental()

            every { rentalDomainService.getOrThrow(rentalId) } returns rental
            every { rentalPaymentRepository.findByRentalId(rentalId) } returns null
            every { paymentGateway.approve(any()) } throws PaymentFailedException("카드 한도 초과")
            every { rentalPaymentRepository.save(any()) } answers { firstArg() }

            val command = ProcessPaymentCommand(
                rentalId = rentalId,
                renterId = renterId,
                paymentKey = "failKey",
                orderId = "RC-1001-1713063600000",
                amount = 120000L,
                paymentMethod = PaymentMethod.CARD,
            )

            shouldThrow<PaymentFailedException> {
                useCase.execute(command)
            }

            verify { rentalPaymentRepository.save(match { it.status == PaymentStatus.FAILED }) }
        }
    }
})
