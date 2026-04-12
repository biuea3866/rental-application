package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
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

class GetProductDetailUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val productPriceRepository = mockk<ProductPriceRepository>()
    val productImageRepository = mockk<ProductImageRepository>()
    val useCase = GetProductDetailUseCase(
        productRepository = productRepository,
        productPriceRepository = productPriceRepository,
        productImageRepository = productImageRepository,
    )

    Given("상품 상세 조회를 요청할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            every { productRepository.findById(999L) } returns null

            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(productId = 999L, requestUserId = null)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("AVAILABLE 상태의 상품을 비로그인 사용자가 조회하면") {
            val product = Product(
                productId = 1L,
                userId = 100L,
                name = "맥북 프로",
                description = "최신형 맥북 프로입니다",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                status = ProductStatus.AVAILABLE,
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
                ProductImage(
                    productImageId = 21L,
                    productId = 1L,
                    objectKey = "products/uuid-002.jpg",
                    originalFilename = "back.jpg",
                    sortOrder = 2,
                ),
            )

            every { productRepository.findById(1L) } returns product
            every { productPriceRepository.findByProductId(1L) } returns prices
            every { productImageRepository.findByProductId(1L) } returns images

            val result = useCase.execute(productId = 1L, requestUserId = null)

            Then("상품 기본 정보가 반환된다") {
                result.id shouldBe 1L
                result.name shouldBe "맥북 프로"
                result.description shouldBe "최신형 맥북 프로입니다"
                result.categoryCode shouldBe "ELECTRONICS"
                result.condition shouldBe ProductCondition.LIKE_NEW
                result.status shouldBe ProductStatus.AVAILABLE
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
                result.images shouldHaveSize 2
                result.images[0].objectKey shouldBe "products/uuid-001.jpg"
                result.images[0].originalFilename shouldBe "front.jpg"
                result.images[0].sortOrder shouldBe 1.toShort()
            }
        }

        When("RENTED 상태의 상품을 조회하면") {
            val product = Product(
                productId = 2L,
                userId = 100L,
                name = "아이패드 에어",
                description = "아이패드 에어입니다",
                categoryCode = "ELECTRONICS",
                status = ProductStatus.RENTED,
                depositAmount = 200000L,
            )

            every { productRepository.findById(2L) } returns product
            every { productPriceRepository.findByProductId(2L) } returns emptyList()
            every { productImageRepository.findByProductId(2L) } returns emptyList()

            val result = useCase.execute(productId = 2L, requestUserId = null)

            Then("정상적으로 조회된다") {
                result.id shouldBe 2L
                result.status shouldBe ProductStatus.RENTED
            }
        }

        When("DRAFT 상태의 상품을 소유자가 조회하면") {
            val product = Product(
                productId = 3L,
                userId = 100L,
                name = "테스트 상품",
                description = "테스트 설명",
                categoryCode = "ELECTRONICS",
                status = ProductStatus.DRAFT,
                depositAmount = 100000L,
            )

            every { productRepository.findById(3L) } returns product
            every { productPriceRepository.findByProductId(3L) } returns emptyList()
            every { productImageRepository.findByProductId(3L) } returns emptyList()

            val result = useCase.execute(productId = 3L, requestUserId = 100L)

            Then("소유자는 정상적으로 조회할 수 있다") {
                result.id shouldBe 3L
                result.status shouldBe ProductStatus.DRAFT
            }
        }

        When("DRAFT 상태의 상품을 비소유자가 조회하면") {
            val product = Product(
                productId = 4L,
                userId = 100L,
                name = "테스트 상품",
                description = "테스트 설명",
                categoryCode = "ELECTRONICS",
                status = ProductStatus.DRAFT,
                depositAmount = 100000L,
            )

            every { productRepository.findById(4L) } returns product

            Then("접근 권한 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(productId = 4L, requestUserId = 200L)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }

        When("DRAFT 상태의 상품을 비로그인 사용자가 조회하면") {
            val product = Product(
                productId = 5L,
                userId = 100L,
                name = "테스트 상품",
                description = "테스트 설명",
                categoryCode = "ELECTRONICS",
                status = ProductStatus.DRAFT,
                depositAmount = 100000L,
            )

            every { productRepository.findById(5L) } returns product

            Then("접근 권한 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(productId = 5L, requestUserId = null)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }

        When("UNDER_REVIEW 상태의 상품을 소유자가 조회하면") {
            val product = Product(
                productId = 6L,
                userId = 100L,
                name = "심사중 상품",
                description = "심사중 설명",
                categoryCode = "ELECTRONICS",
                status = ProductStatus.UNDER_REVIEW,
                depositAmount = 300000L,
            )

            every { productRepository.findById(6L) } returns product
            every { productPriceRepository.findByProductId(6L) } returns emptyList()
            every { productImageRepository.findByProductId(6L) } returns emptyList()

            val result = useCase.execute(productId = 6L, requestUserId = 100L)

            Then("소유자는 정상적으로 조회할 수 있다") {
                result.id shouldBe 6L
                result.status shouldBe ProductStatus.UNDER_REVIEW
            }
        }

        When("UNDER_REVIEW 상태의 상품을 비소유자가 조회하면") {
            val product = Product(
                productId = 7L,
                userId = 100L,
                name = "심사중 상품",
                description = "심사중 설명",
                categoryCode = "ELECTRONICS",
                status = ProductStatus.UNDER_REVIEW,
                depositAmount = 300000L,
            )

            every { productRepository.findById(7L) } returns product

            Then("접근 권한 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(productId = 7L, requestUserId = 200L)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }
    }
})
