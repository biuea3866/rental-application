package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.rental.RentalWithPayment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class GetRentalDetailUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = GetRentalDetailUseCase(rentalDomainService)

    val now = ZonedDateTime.now()
    val renterId = 10L
    val lenderId = 20L
    val rentalId = 1L

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    fun createRental(): Rental = Rental.create(
        renterId = renterId,
        lenderId = lenderId,
        productId = 42L,
        startDate = now.plusDays(1),
        endDate = now.plusDays(8),
        totalAmount = 70_000L,
        depositAmount = 50_000L,
        deliveryInfo = deliveryInfo,
    )

    fun createPayment(rentalId: Long = 1L): RentalPayment = RentalPayment.create(
        rentalId = rentalId,
        amount = 70_000L,
        paymentMethod = PaymentMethod.CARD,
        orderId = "RC-0001-1713063600000",
    )

    beforeEach { clearMocks(rentalDomainService) }

    Given("대여 상세 조회 시") {

        When("참여자(renterId)가 조회하면") {
            Then("Rental + RentalPayment 정보가 반환된다") {
                val rental = createRental()
                rental.approve()
                val payment = createPayment()
                payment.complete("ext-payment-id-001")

                every { rentalDomainService.getRentalDetail(rentalId) } returns RentalWithPayment(
                    rental = rental,
                    payment = payment,
                )

                val command = GetRentalDetailCommand(rentalId = rentalId, userId = renterId)
                val result = useCase.execute(command)

                result.rentalId shouldBe rental.id
                result.renterId shouldBe renterId
                result.lenderId shouldBe lenderId
                result.status shouldBe RentalStatus.APPROVED
                val paymentResult = result.payment.shouldNotBeNull()
                paymentResult.paymentMethod shouldBe PaymentMethod.CARD
                paymentResult.status shouldBe PaymentStatus.COMPLETED
                verify(exactly = 1) { rentalDomainService.getRentalDetail(rentalId) }
            }
        }

        When("참여자(lenderId)가 조회하면") {
            Then("Rental + RentalPayment 정보가 반환된다") {
                val rental = createRental()

                every { rentalDomainService.getRentalDetail(rentalId) } returns RentalWithPayment(
                    rental = rental,
                    payment = null,
                )

                val command = GetRentalDetailCommand(rentalId = rentalId, userId = lenderId)
                val result = useCase.execute(command)

                result.lenderId shouldBe lenderId
                result.payment.shouldBeNull()
            }
        }

        When("결제 정보가 없는 대여를 조회하면") {
            Then("payment가 null인 RentalDetailResult가 반환된다") {
                val rental = createRental()

                every { rentalDomainService.getRentalDetail(rentalId) } returns RentalWithPayment(
                    rental = rental,
                    payment = null,
                )

                val command = GetRentalDetailCommand(rentalId = rentalId, userId = renterId)
                val result = useCase.execute(command)

                result.payment.shouldBeNull()
                result.status shouldBe RentalStatus.REQUESTED
            }
        }

        When("대여에 참여하지 않은 사용자가 조회하면") {
            Then("FORBIDDEN 예외가 발생한다") {
                val rental = createRental()
                val notParticipantId = 999L

                every { rentalDomainService.getRentalDetail(rentalId) } returns RentalWithPayment(
                    rental = rental,
                    payment = null,
                )

                val command = GetRentalDetailCommand(rentalId = rentalId, userId = notParticipantId)
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }

        When("존재하지 않는 대여를 조회하면") {
            Then("RentalNotFoundException 예외가 발생한다") {
                val notExistRentalId = 999L

                every { rentalDomainService.getRentalDetail(notExistRentalId) } throws RentalNotFoundException(
                    "대여를 찾을 수 없습니다. rentalId=$notExistRentalId"
                )

                val command = GetRentalDetailCommand(rentalId = notExistRentalId, userId = renterId)
                shouldThrow<RentalNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
