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

class ApproveRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val useCase = ApproveRentalUseCase(rentalDomainService)

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

    Given("대여 승인 요청을 할 때") {

        When("등록자가 REQUESTED 상태의 대여를 승인하면") {
            Then("RentalDomainService.approveRental()이 호출되고 APPROVED 상태로 전이된다") {
                val lenderId = 20L
                val rentalId = 1L
                val rental = createRequestedRental(rentalId = rentalId, lenderId = lenderId)

                val command = ApproveRentalCommand(
                    rentalId = rentalId,
                    userId = lenderId,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalDomainService.approveRental(rental, lenderId) } answers {
                    rental.approve()
                    rental
                }

                useCase.execute(command)

                rental.status shouldBe RentalStatus.APPROVED
                verify(exactly = 1) { rentalDomainService.getRentalById(rentalId) }
                verify(exactly = 1) { rentalDomainService.approveRental(rental, lenderId) }
            }
        }

        When("등록자가 아닌 사용자가 승인을 시도하면") {
            Then("FORBIDDEN 예외가 발생한다") {
                val lenderId = 20L
                val notLenderId = 99L
                val rentalId = 1L
                val rental = createRequestedRental(rentalId = rentalId, lenderId = lenderId)

                val command = ApproveRentalCommand(
                    rentalId = rentalId,
                    userId = notLenderId,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalDomainService.approveRental(rental, notLenderId) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                    message = "대여 승인 권한이 없습니다. rentalId=$rentalId",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
                verify(exactly = 1) { rentalDomainService.approveRental(rental, notLenderId) }
            }
        }

        When("REQUESTED 상태가 아닌 대여를 승인하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
                val lenderId = 20L
                val rentalId = 1L
                val rental = createRequestedRental(rentalId = rentalId, lenderId = lenderId)
                rental.approve()

                val command = ApproveRentalCommand(
                    rentalId = rentalId,
                    userId = lenderId,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every { rentalDomainService.approveRental(rental, lenderId) } throws InvalidStateTransitionException(
                    "APPROVED에서 APPROVED(으)로 전이할 수 없습니다"
                )

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }

        When("존재하지 않는 대여를 승인하려고 하면") {
            Then("RentalNotFoundException 예외가 발생한다") {
                val rentalId = 999L
                val command = ApproveRentalCommand(
                    rentalId = rentalId,
                    userId = 20L,
                )

                every { rentalDomainService.getRentalById(rentalId) } throws RentalNotFoundException("대여를 찾을 수 없습니다. rentalId=$rentalId")

                shouldThrow<RentalNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
