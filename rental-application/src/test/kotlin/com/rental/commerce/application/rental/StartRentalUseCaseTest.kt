package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
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

class StartRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val rentalRepository = mockk<RentalRepository>()
    val rentalEventPublisher = mockk<RentalEventPublisher>()
    val useCase = StartRentalUseCase(rentalDomainService, rentalRepository, rentalEventPublisher)

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    val renterId = 10L
    val lenderId = 20L

    fun createPaidRental(rentalId: Long = 1L): Rental {
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
        return rental
    }

    beforeEach {
        clearMocks(rentalDomainService, rentalRepository, rentalEventPublisher)
    }

    Given("대여 시작 요청을 할 때") {

        When("등록자(lender)가 PAID 상태의 대여를 시작하면") {
            Then("rental.startRental()이 호출되고 IN_USE 상태로 저장된다") {
                val rental = createPaidRental(rentalId = 1L)

                val command = StartRentalCommand(
                    rentalId = 1L,
                    userId = lenderId,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental
                every { rentalRepository.save(rental) } returns rental
                every { rentalEventPublisher.publishAll(any()) } just runs

                useCase.execute(command)

                rental.status shouldBe RentalStatus.IN_USE
                verify(exactly = 1) { rentalDomainService.getRentalById(1L) }
                verify(exactly = 1) { rentalRepository.save(rental) }
            }
        }

        When("등록자가 아닌 사용자(대여자)가 시작을 시도하면") {
            Then("FORBIDDEN 예외가 발생한다") {
                val rental = createPaidRental(rentalId = 1L)

                val command = StartRentalCommand(
                    rentalId = 1L,
                    userId = renterId, // 대여자가 시도 — 등록자만 가능
                )

                every { rentalDomainService.getRentalById(1L) } returns rental

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
                verify(exactly = 0) { rentalRepository.save(any()) }
            }
        }

        When("PAID가 아닌 상태(REQUESTED)의 대여를 시작하려고 하면") {
            Then("InvalidStateTransitionException 예외가 발생한다") {
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

                val command = StartRentalCommand(
                    rentalId = 1L,
                    userId = lenderId,
                )

                every { rentalDomainService.getRentalById(1L) } returns rental

                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
                verify(exactly = 0) { rentalRepository.save(any()) }
            }
        }
    }
})
