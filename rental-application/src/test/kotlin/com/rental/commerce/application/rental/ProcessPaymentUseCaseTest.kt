package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.AlreadyPaidException
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentResult
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
import com.rental.commerce.domain.rental.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.ZonedDateTime

class ProcessPaymentUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val rentalPaymentRepository = mockk<RentalPaymentRepository>()
    val paymentGateway = mockk<PaymentGateway>()
    val rentalEventPublisher = mockk<RentalEventPublisher>()

    val useCase = ProcessPaymentUseCase(
        rentalDomainService = rentalDomainService,
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
    val productId = 42L
    val rentalId = 1001L
    val orderId = "RC-1001-1713063600000"
    val paymentKey = "5zJ4xY7m0kODnyRpQWGrN2eqXgarbMe7zEX5sGR0"
    val amount = 120_000L
    val now = ZonedDateTime.now()

    fun buildApprovedRental(): Rental {
        val rental = Rental.create(
            renterId = renterId,
            lenderId = lenderId,
            productId = productId,
            startDate = now.plusDays(1),
            endDate = now.plusDays(8),
            totalAmount = amount,
            depositAmount = 50_000L,
            deliveryInfo = deliveryInfo,
        )
        rental.approve()
        return rental
    }

    beforeEach {
        clearMocks(rentalDomainService, rentalPaymentRepository, paymentGateway, rentalEventPublisher)
    }

    Given("결제 처리 UseCase 실행 시") {

        When("APPROVED 상태의 대여에 대해 정상 결제 요청이 들어오면") {
            Then("PaymentGateway.requestPayment()가 호출되고 rental.markPaid() 후 RentalPayment(COMPLETED) 가 저장된다") {
                val rental = buildApprovedRental()

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                val paymentResult = PaymentResult(
                    success = true,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    approvedAt = now,
                    failureMessage = null,
                )

                val savedPayment = RentalPayment.create(
                    rentalId = rentalId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                    orderId = orderId,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalPaymentRepository.findByRentalId(rentalId) } returns null
                every {
                    paymentGateway.requestPayment(
                        PaymentApproveRequest(
                            orderId = orderId,
                            amount = amount,
                            paymentKey = paymentKey,
                        )
                    )
                } returns paymentResult
                every { rentalPaymentRepository.save(any()) } returns savedPayment
                every { rentalEventPublisher.publishAll(any()) } just runs

                val result = useCase.execute(command)

                result shouldNotBe null
                result.rentalStatus shouldBe RentalStatus.PAID
                result.paymentStatus shouldBe PaymentStatus.COMPLETED

                verify(exactly = 1) {
                    paymentGateway.requestPayment(
                        PaymentApproveRequest(
                            orderId = orderId,
                            amount = amount,
                            paymentKey = paymentKey,
                        )
                    )
                }
                verify(exactly = 1) { rentalPaymentRepository.save(any()) }
            }
        }

        When("이미 결제가 완료된 rental(PAID 상태)에 결제 요청이 들어오면") {
            Then("PaymentGateway 호출 없이 기존 RentalPayment 를 반환한다 (멱등성)") {
                val rental = buildApprovedRental()
                rental.markPaid()

                val existingPayment = RentalPayment.create(
                    rentalId = rentalId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                    orderId = orderId,
                )

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalPaymentRepository.findByRentalId(rentalId) } returns existingPayment

                val result = useCase.execute(command)

                result shouldNotBe null
                verify(exactly = 0) { paymentGateway.requestPayment(any()) }
                verify(exactly = 0) { rentalPaymentRepository.save(any()) }
            }
        }

        When("PaymentGateway 에서 결제 실패가 반환되면") {
            Then("RentalPayment(FAILED) 가 저장되고 PaymentFailedException 이 발생한다") {
                val rental = buildApprovedRental()

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                val failureResult = PaymentResult(
                    success = false,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    approvedAt = null,
                    failureMessage = "잔액 부족",
                )

                val savedFailedPayment = RentalPayment.create(
                    rentalId = rentalId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                    orderId = orderId,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalPaymentRepository.findByRentalId(rentalId) } returns null
                every {
                    paymentGateway.requestPayment(any())
                } returns failureResult
                every { rentalPaymentRepository.save(any()) } returns savedFailedPayment

                shouldThrow<PaymentFailedException> {
                    useCase.execute(command)
                }

                verify(exactly = 1) { rentalPaymentRepository.save(any()) }
            }
        }

        When("REQUESTED 상태의 대여에 결제를 시도하면") {
            Then("InvalidStateTransitionException 이 발생한다") {
                val requestedRental = Rental.create(
                    renterId = renterId,
                    lenderId = lenderId,
                    productId = productId,
                    startDate = now.plusDays(1),
                    endDate = now.plusDays(8),
                    totalAmount = amount,
                    depositAmount = 50_000L,
                    deliveryInfo = deliveryInfo,
                )

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns requestedRental
                every { rentalPaymentRepository.findByRentalId(rentalId) } returns null

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }

                verify(exactly = 0) { paymentGateway.requestPayment(any()) }
            }
        }

        When("대여 신청자가 아닌 다른 사용자가 결제를 시도하면") {
            Then("FORBIDDEN BusinessException 이 발생한다") {
                val rental = buildApprovedRental()

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = 999L, // 다른 사용자
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalPaymentRepository.findByRentalId(rentalId) } returns null

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }

                exception.errorCode shouldBe ErrorCode.FORBIDDEN
                verify(exactly = 0) { paymentGateway.requestPayment(any()) }
            }
        }
    }
})
