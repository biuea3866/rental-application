package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.common.RentalPeriodConflictException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
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

class RequestRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val productRepository = mockk<ProductRepository>()
    val rentalEventPublisher = mockk<RentalEventPublisher>()
    val useCase = RequestRentalUseCase(rentalDomainService, productRepository, rentalEventPublisher)

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    beforeEach {
        clearMocks(rentalDomainService, productRepository, rentalEventPublisher)
    }

    Given("대여 신청을 할 때") {

        When("정상적으로 대여 신청을 하면") {
            Then("RentalDomainService.createRental()이 호출되고 결과가 반환된다") {
                val renterId = 10L
                val productId = 42L
                val startDate = ZonedDateTime.now().plusDays(1)
                val endDate = ZonedDateTime.now().plusDays(8)

                val product = Product(
                    productId = productId,
                    userId = 20L, // renterId != lenderId
                    name = "캠핑 텐트 A",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 50_000L,
                )

                val expectedRental = Rental.create(
                    renterId = renterId,
                    lenderId = 20L,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    totalAmount = 70_000L,
                    depositAmount = 50_000L,
                    deliveryInfo = deliveryInfo,
                )

                val command = RequestRentalCommand(
                    renterId = renterId,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    dailyPrice = 10_000L,
                    deliveryInfo = deliveryInfo,
                )

                every { productRepository.findById(productId) } returns product
                every {
                    rentalDomainService.createRental(
                        renterId = renterId,
                        lenderId = 20L,
                        productId = productId,
                        startDate = startDate,
                        endDate = endDate,
                        dailyPrice = 10_000L,
                        depositAmount = 50_000L,
                        deliveryInfo = deliveryInfo,
                    )
                } returns expectedRental
                every { rentalEventPublisher.publishAll(any()) } just runs

                val result = useCase.execute(command)

                result shouldNotBe null
                result.status shouldBe RentalStatus.REQUESTED
                verify(exactly = 1) {
                    rentalDomainService.createRental(
                        renterId = renterId,
                        lenderId = 20L,
                        productId = productId,
                        startDate = startDate,
                        endDate = endDate,
                        dailyPrice = 10_000L,
                        depositAmount = 50_000L,
                        deliveryInfo = deliveryInfo,
                    )
                }
            }
        }

        When("대여 기간이 중복된 경우") {
            Then("RentalPeriodConflictException 예외가 발생한다") {
                val renterId = 10L
                val productId = 42L
                val startDate = ZonedDateTime.now().plusDays(1)
                val endDate = ZonedDateTime.now().plusDays(8)

                val product = Product(
                    productId = productId,
                    userId = 20L,
                    name = "캠핑 텐트 A",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 50_000L,
                )

                val command = RequestRentalCommand(
                    renterId = renterId,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    dailyPrice = 10_000L,
                    deliveryInfo = deliveryInfo,
                )

                every { productRepository.findById(productId) } returns product
                every {
                    rentalDomainService.createRental(
                        renterId = renterId,
                        lenderId = 20L,
                        productId = productId,
                        startDate = startDate,
                        endDate = endDate,
                        dailyPrice = 10_000L,
                        depositAmount = 50_000L,
                        deliveryInfo = deliveryInfo,
                    )
                } throws RentalPeriodConflictException()

                val exception = shouldThrow<RentalPeriodConflictException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.RENTAL_PERIOD_CONFLICT
            }
        }

        When("자기 자신의 상품을 대여 신청하면") {
            Then("FORBIDDEN 예외가 발생한다") {
                val userId = 10L
                val productId = 42L
                val startDate = ZonedDateTime.now().plusDays(1)
                val endDate = ZonedDateTime.now().plusDays(8)

                val product = Product(
                    productId = productId,
                    userId = userId, // renterId == lenderId
                    name = "내 캠핑 텐트",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 50_000L,
                )

                val command = RequestRentalCommand(
                    renterId = userId,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    dailyPrice = 10_000L,
                    deliveryInfo = deliveryInfo,
                )

                every { productRepository.findById(productId) } returns product

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }

        When("존재하지 않는 상품에 대여 신청하면") {
            Then("ResourceNotFoundException 예외가 발생한다") {
                val renterId = 10L
                val productId = 999L
                val startDate = ZonedDateTime.now().plusDays(1)
                val endDate = ZonedDateTime.now().plusDays(8)

                val command = RequestRentalCommand(
                    renterId = renterId,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    dailyPrice = 10_000L,
                    deliveryInfo = deliveryInfo,
                )

                every { productRepository.findById(productId) } returns null

                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
