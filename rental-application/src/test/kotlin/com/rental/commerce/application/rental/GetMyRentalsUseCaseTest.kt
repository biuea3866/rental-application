package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalQueryCondition
import com.rental.commerce.domain.rental.RentalStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZonedDateTime

class GetMyRentalsUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val productDomainService = mockk<ProductDomainService>()
    val useCase = GetMyRentalsUseCase(rentalDomainService, productDomainService)

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    fun createRental(renterId: Long = 10L, lenderId: Long = 20L, status: RentalStatus = RentalStatus.REQUESTED): Rental {
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
        if (status == RentalStatus.APPROVED) rental.approve()
        return rental
    }

    beforeEach { clearMocks(rentalDomainService, productDomainService) }

    fun mockProductNoImage(productId: Long, name: String): Product =
        mockk<Product>().also {
            every { it.productId } returns productId
            every { it.name } returns name
        }

    Given("내 대여 목록 조회 시") {

        When("상태 필터 없이 전체 목록을 조회하면") {
            Then("renterId 기준으로 모든 대여가 페이지네이션되어 반환된다") {
                val userId = 10L
                val rentals = listOf(
                    createRental(renterId = userId, status = RentalStatus.REQUESTED),
                    createRental(renterId = userId, status = RentalStatus.APPROVED),
                )
                val conditionSlot = slot<RentalQueryCondition>()

                every { rentalDomainService.getMyRentals(capture(conditionSlot)) } returns PageResult(
                    content = rentals,
                    totalElements = 2L,
                    totalPages = 1,
                )
                every { productDomainService.getProductById(42L) } returns mockProductNoImage(42L, "캠핑 텐트")

                val command = GetMyRentalsCommand(
                    userId = userId,
                    pageQuery = PageQuery(page = 0, size = 20),
                )
                val result = useCase.execute(command)

                result.content shouldHaveSize 2
                result.totalElements shouldBe 2L
                result.totalPages shouldBe 1
                conditionSlot.captured.userId shouldBe userId
                conditionSlot.captured.statusFilter shouldBe null
                conditionSlot.captured.pageQuery shouldBe PageQuery(page = 0, size = 20)
                result.content[0].productName shouldBe "캠핑 텐트"
                verify(exactly = 1) { rentalDomainService.getMyRentals(any()) }
            }
        }

        When("REQUESTED 상태 필터로 조회하면") {
            Then("REQUESTED 상태의 대여만 반환된다") {
                val userId = 10L
                val rentals = listOf(createRental(renterId = userId, status = RentalStatus.REQUESTED))
                val conditionSlot = slot<RentalQueryCondition>()

                every { rentalDomainService.getMyRentals(capture(conditionSlot)) } returns PageResult(
                    content = rentals,
                    totalElements = 1L,
                    totalPages = 1,
                )
                every { productDomainService.getProductById(42L) } returns mockProductNoImage(42L, "캠핑 텐트")

                val command = GetMyRentalsCommand(
                    userId = userId,
                    statusFilter = RentalStatus.REQUESTED,
                    pageQuery = PageQuery(page = 0, size = 10),
                )
                val result = useCase.execute(command)

                result.content shouldHaveSize 1
                result.content[0].status shouldBe RentalStatus.REQUESTED
                conditionSlot.captured.statusFilter shouldBe RentalStatus.REQUESTED
            }
        }

        When("결과가 없으면") {
            Then("빈 PageResult를 반환한다") {
                val userId = 999L

                every { rentalDomainService.getMyRentals(any()) } returns PageResult(
                    content = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )

                val command = GetMyRentalsCommand(userId = userId)
                val result = useCase.execute(command)

                result.content shouldHaveSize 0
                result.totalElements shouldBe 0L
            }
        }

        When("2페이지를 요청하면") {
            Then("pageQuery가 DomainService에 그대로 전달된다") {
                val userId = 10L
                val conditionSlot = slot<RentalQueryCondition>()

                every { rentalDomainService.getMyRentals(capture(conditionSlot)) } returns PageResult(
                    content = emptyList(),
                    totalElements = 25L,
                    totalPages = 3,
                )

                val command = GetMyRentalsCommand(
                    userId = userId,
                    pageQuery = PageQuery(page = 2, size = 10),
                )
                useCase.execute(command)

                conditionSlot.captured.pageQuery shouldBe PageQuery(page = 2, size = 10)
            }
        }

        When("대여 목록의 상품 이름과 썸네일 URL을 조회하면") {
            Then("productName과 productThumbnailUrl이 RentalSummaryResult에 포함된다") {
                val userId = 10L
                val rental = createRental(renterId = userId, status = RentalStatus.REQUESTED)
                val product = mockk<Product>().also {
                    every { it.productId } returns 42L
                    every { it.name } returns "최신 캠핑 텐트"
                }

                every { rentalDomainService.getMyRentals(any()) } returns PageResult(
                    content = listOf(rental),
                    totalElements = 1L,
                    totalPages = 1,
                )
                every { productDomainService.getProductById(42L) } returns product

                val command = GetMyRentalsCommand(userId = userId)
                val result = useCase.execute(command)

                result.content shouldHaveSize 1
                result.content[0].productName shouldBe "최신 캠핑 텐트"
                result.content[0].productThumbnailUrl.shouldBeNull()
            }
        }
    }
})
