package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.RentalPeriodConflictException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.common.RentalStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class RequestRentalUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val productDomainService = mockk<ProductDomainService>()
    val useCase = RequestRentalUseCase(rentalDomainService, productDomainService)

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    val lenderId = 20L

    fun buildProduct(userId: Long = lenderId, depositAmount: Long? = 50_000L): Product {
        val product = mockk<Product>()
        every { product.userId } returns userId
        every { product.depositAmount } returns depositAmount
        justRun { product.validateNotOwnedBy(any()) }
        justRun { product.validateAvailableForRental() }
        return product
    }

    beforeEach {
        clearMocks(rentalDomainService, productDomainService)
    }

    Given("대여 신청을 할 때") {

        When("정상적으로 대여 신청을 하면") {
            Then("ProductDomainService로 상품 조회 후 RentalDomainService.createRental()이 호출된다") {
                val renterId = 10L
                val productId = 42L
                val startDate = ZonedDateTime.now().plusDays(1)
                val endDate = ZonedDateTime.now().plusDays(8)
                val product = buildProduct()

                val expectedRental = Rental.create(
                    renterId = renterId,
                    lenderId = lenderId,
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

                every { productDomainService.getProductById(productId) } returns product
                every {
                    rentalDomainService.createRental(
                        renterId = renterId,
                        lenderId = lenderId,
                        productId = productId,
                        startDate = startDate,
                        endDate = endDate,
                        dailyPrice = 10_000L,
                        depositAmount = 50_000L,
                        deliveryInfo = deliveryInfo,
                    )
                } returns expectedRental

                val result = useCase.execute(command)

                result shouldNotBe null
                result.status shouldBe RentalStatus.REQUESTED
                verify(exactly = 1) { productDomainService.getProductById(productId) }
                verify(exactly = 1) {
                    rentalDomainService.createRental(
                        renterId = renterId,
                        lenderId = lenderId,
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
                val product = buildProduct()

                val command = RequestRentalCommand(
                    renterId = renterId,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    dailyPrice = 10_000L,
                    deliveryInfo = deliveryInfo,
                )

                every { productDomainService.getProductById(productId) } returns product
                every {
                    rentalDomainService.createRental(
                        renterId = renterId,
                        lenderId = lenderId,
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
                val userId = 20L
                val productId = 42L
                val startDate = ZonedDateTime.now().plusDays(1)
                val endDate = ZonedDateTime.now().plusDays(8)

                val product = mockk<Product>()
                every { product.userId } returns userId
                every { product.depositAmount } returns 50_000L
                every { product.validateNotOwnedBy(userId) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                    message = "자신의 상품은 대여 신청할 수 없습니다.",
                )

                val command = RequestRentalCommand(
                    renterId = userId,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    dailyPrice = 10_000L,
                    deliveryInfo = deliveryInfo,
                )

                every { productDomainService.getProductById(productId) } returns product

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

                every { productDomainService.getProductById(productId) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                    message = "상품을 찾을 수 없습니다 (id=$productId)",
                )

                shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
