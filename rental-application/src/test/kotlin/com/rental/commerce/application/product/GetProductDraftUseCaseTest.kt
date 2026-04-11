package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductPrice
import com.rental.commerce.domain.product.ProductPriceRepository
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetProductDraftUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val productPriceRepository = mockk<ProductPriceRepository>()
    val productImageRepository = mockk<ProductImageRepository>()
    val useCase = GetProductDraftUseCase(
        productRepository = productRepository,
        productPriceRepository = productPriceRepository,
        productImageRepository = productImageRepository,
    )

    Given("DRAFT 상품 상세 조회를 요청할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            every { productRepository.findById(999L) } returns null

            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(userId = 1L, productId = 999L)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("다른 사용자의 상품을 조회하려고 하면") {
            val product = Product(
                productId = 1L,
                userId = 100L,
                status = ProductStatus.DRAFT,
            )

            every { productRepository.findById(1L) } returns product

            Then("PRODUCT_OWNERSHIP_DENIED 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(userId = 200L, productId = 1L)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }

        When("정상적으로 본인의 DRAFT 상품을 조회하면") {
            val product = Product(
                productId = 1L,
                userId = 1L,
                name = "맥북 프로",
                description = "최신형 맥북 프로입니다",
                categoryCode = "ELECTRONICS",
                status = ProductStatus.DRAFT,
                currentDraftStep = 3,
                depositAmount = 500000L,
            )

            val prices = listOf(
                ProductPrice(
                    productPriceId = 10L,
                    productId = 1L,
                    rentalUnit = RentalUnit.DAILY,
                    priceAmount = 30000L,
                ),
                ProductPrice(
                    productPriceId = 11L,
                    productId = 1L,
                    rentalUnit = RentalUnit.MONTHLY,
                    priceAmount = 500000L,
                ),
            )

            val images = listOf(
                ProductImage(
                    productImageId = 20L,
                    productId = 1L,
                    objectKey = "products/uuid-001.jpg",
                    originalFilename = "front.jpg",
                    sortOrder = 1,
                ),
            )

            every { productRepository.findById(1L) } returns product
            every { productPriceRepository.findByProductId(1L) } returns prices
            every { productImageRepository.findByProductId(1L) } returns images

            val result = useCase.execute(userId = 1L, productId = 1L)

            Then("상품 기본 정보가 반환된다") {
                result.productId shouldBe 1L
                result.name shouldBe "맥북 프로"
                result.description shouldBe "최신형 맥북 프로입니다"
                result.categoryCode shouldBe "ELECTRONICS"
                result.status shouldBe ProductStatus.DRAFT
                result.currentDraftStep shouldBe 3
                result.depositAmount shouldBe 500000L
            }

            Then("가격 정보가 포함된다") {
                result.prices shouldHaveSize 2
                result.prices[0].rentalUnit shouldBe RentalUnit.DAILY
                result.prices[0].priceAmount shouldBe 30000L
                result.prices[1].rentalUnit shouldBe RentalUnit.MONTHLY
                result.prices[1].priceAmount shouldBe 500000L
            }

            Then("이미지 정보가 포함된다") {
                result.images shouldHaveSize 1
                result.images[0].objectKey shouldBe "products/uuid-001.jpg"
                result.images[0].originalFilename shouldBe "front.jpg"
                result.images[0].sortOrder shouldBe 1.toShort()
            }
        }

        When("가격과 이미지가 없는 DRAFT 상품을 조회하면") {
            val product = Product(
                productId = 2L,
                userId = 1L,
                status = ProductStatus.DRAFT,
                currentDraftStep = 1,
            )

            every { productRepository.findById(2L) } returns product
            every { productPriceRepository.findByProductId(2L) } returns emptyList()
            every { productImageRepository.findByProductId(2L) } returns emptyList()

            val result = useCase.execute(userId = 1L, productId = 2L)

            Then("빈 가격/이미지 목록이 반환된다") {
                result.prices shouldHaveSize 0
                result.images shouldHaveSize 0
            }
        }
    }
})
