package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class ProcessPaymentUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()

    val useCase = ProcessPaymentUseCase(
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
        clearMocks(rentalDomainService)
    }

    Given("결제 처리 UseCase 실행 시") {

        When("APPROVED 상태의 대여에 대해 정상 결제 요청이 들어오면") {
            Then("RentalDomainService.processPayment()가 호출되고 결과가 반환된다") {
                val rental = buildApprovedRental()

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                val completedPayment = RentalPayment.create(
                    rentalId = rentalId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                    orderId = orderId,
                ).also { it.complete(paymentKey) }

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                } answers {
                    rental.markPaid()
                    completedPayment
                }

                val result = useCase.execute(command)

                result shouldNotBe null
                result.rentalStatus shouldBe RentalStatus.PAID
                result.paymentStatus shouldBe PaymentStatus.COMPLETED

                verify(exactly = 1) {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                }
            }
        }

        When("이미 결제가 완료된 rental(PAID 상태)에 결제 요청이 들어오면") {
            Then("DomainService.processPayment()가 기존 결제를 반환한다 (멱등성)") {
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
                every {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                } returns existingPayment

                val result = useCase.execute(command)

                result shouldNotBe null
                verify(exactly = 1) {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                }
            }
        }

        When("PaymentGateway 에서 결제 실패가 반환되면") {
            Then("PaymentFailedException 이 발생한다") {
                val rental = buildApprovedRental()

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                } throws PaymentFailedException(message = "잔액 부족")

                shouldThrow<PaymentFailedException> {
                    useCase.execute(command)
                }
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
                every {
                    rentalDomainService.processPayment(
                        rental = requestedRental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                } throws InvalidStateTransitionException("REQUESTED에서 PAID(으)로 전이할 수 없습니다")

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }

        When("대여 신청자가 아닌 다른 사용자가 결제를 시도하면") {
            Then("FORBIDDEN BusinessException 이 발생한다") {
                val rental = buildApprovedRental()

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = 999L,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = 999L,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                    message = "결제 권한이 없습니다. rentalId=${rental.id}",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }

                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }

        When("결제 금액과 대여 금액이 다른 경우 DomainService가 예외를 던지면") {
            Then("PaymentFailedException 이 전파된다") {
                val rental = buildApprovedRental()
                val wrongAmount = amount - 1L

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = wrongAmount,
                    paymentMethod = PaymentMethod.CARD,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = wrongAmount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.CARD,
                    )
                } throws PaymentFailedException(message = "결제 금액 불일치")

                shouldThrow<PaymentFailedException> {
                    useCase.execute(command)
                }
            }
        }

        When("결제 방법이 KAKAO_PAY인 경우 정상 결제 요청이 처리되면") {
            Then("ProcessPaymentResult가 반환되고 paymentStatus가 COMPLETED이다") {
                val rental = buildApprovedRental()

                val command = ProcessPaymentCommand(
                    rentalId = rentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.KAKAO_PAY,
                )

                val completedPayment = RentalPayment.create(
                    rentalId = rentalId,
                    amount = amount,
                    paymentMethod = PaymentMethod.KAKAO_PAY,
                    orderId = orderId,
                ).also { it.complete(paymentKey) }

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every {
                    rentalDomainService.processPayment(
                        rental = rental,
                        renterId = renterId,
                        orderId = orderId,
                        amount = amount,
                        paymentKey = paymentKey,
                        paymentMethod = PaymentMethod.KAKAO_PAY,
                    )
                } answers {
                    rental.markPaid()
                    completedPayment
                }

                val result = useCase.execute(command)

                result.paymentStatus shouldBe PaymentStatus.COMPLETED
                result.rentalStatus shouldBe RentalStatus.PAID
            }
        }

        When("존재하지 않는 rentalId로 결제를 시도하면") {
            Then("DomainService의 getRentalById가 던진 예외가 그대로 전파된다") {
                val nonExistentRentalId = 9999L
                val command = ProcessPaymentCommand(
                    rentalId = nonExistentRentalId,
                    renterId = renterId,
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = PaymentMethod.CARD,
                )

                every { rentalDomainService.getRentalById(nonExistentRentalId) } throws
                    RentalNotFoundException("대여를 찾을 수 없습니다. rentalId=$nonExistentRentalId")

                shouldThrow<RentalNotFoundException> {
                    useCase.execute(command)
                }

                verify(exactly = 0) {
                    rentalDomainService.processPayment(any(), any(), any(), any(), any(), any())
                }
            }
        }
    }
})
