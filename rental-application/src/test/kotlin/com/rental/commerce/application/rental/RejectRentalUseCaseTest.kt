package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.common.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class RejectRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = RejectRentalUseCase(rentalDomainService)

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    fun createRequestedRental(rentalId: Long = 1L, renterId: Long = 10L, lenderId: Long = 20L): Rental =
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

    Given("대여 거절 요청을 할 때") {

        When("등록자가 REQUESTED 상태의 대여를 거절하면") {
            Then("RentalDomainService.rejectRental()이 호출되고 CANCELLED 상태로 전이된다") {
                val lenderId = 20L
                val rentalId = 1L
                val reason = "해당 기간에 다른 일정이 생겼습니다."
                val rental = createRequestedRental(rentalId = rentalId, lenderId = lenderId)

                val command = RejectRentalCommand(
                    rentalId = rentalId,
                    userId = lenderId,
                    reason = reason,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalDomainService.rejectRental(rental, lenderId, reason) } answers {
                    rental.reject(reason)
                    rental
                }

                useCase.execute(command)

                rental.status shouldBe RentalStatus.CANCELLED
                rental.cancelReason shouldBe reason
                verify(exactly = 1) { rentalDomainService.getRentalById(rentalId) }
                verify(exactly = 1) { rentalDomainService.rejectRental(rental, lenderId, reason) }
            }
        }

        When("등록자가 아닌 사용자가 거절을 시도하면") {
            Then("FORBIDDEN 예외가 발생한다") {
                val lenderId = 20L
                val notLenderId = 99L
                val rentalId = 1L
                val reason = "거절 사유입니다."
                val rental = createRequestedRental(rentalId = rentalId, lenderId = lenderId)

                val command = RejectRentalCommand(
                    rentalId = rentalId,
                    userId = notLenderId,
                    reason = reason,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalDomainService.rejectRental(rental, notLenderId, reason) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                    message = "대여 거절 권한이 없습니다. rentalId=$rentalId",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
                verify(exactly = 1) { rentalDomainService.rejectRental(rental, notLenderId, reason) }
            }
        }

        When("REQUESTED 상태가 아닌 대여를 거절하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
                val lenderId = 20L
                val rentalId = 1L
                val reason = "거절 사유입니다."
                val rental = createRequestedRental(rentalId = rentalId, lenderId = lenderId)
                rental.approve()

                val command = RejectRentalCommand(
                    rentalId = rentalId,
                    userId = lenderId,
                    reason = reason,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalDomainService.rejectRental(rental, lenderId, reason) } throws InvalidStateTransitionException(
                    "APPROVED에서 CANCELLED(으)로 거절 전이는 REQUESTED 상태에서만 가능합니다"
                )

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }

        When("존재하지 않는 대여를 거절하려고 하면") {
            Then("RentalNotFoundException 예외가 발생한다") {
                val rentalId = 999L
                val command = RejectRentalCommand(
                    rentalId = rentalId,
                    userId = 20L,
                    reason = "거절 사유입니다.",
                )

                every { rentalDomainService.getRentalById(rentalId) } throws RentalNotFoundException("대여를 찾을 수 없습니다. rentalId=$rentalId")

                shouldThrow<RentalNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
