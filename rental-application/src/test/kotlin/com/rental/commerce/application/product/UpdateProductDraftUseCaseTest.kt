package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductPriceRepository
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class UpdateProductDraftUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val productPriceRepository = mockk<ProductPriceRepository>()
    val productImageRepository = mockk<ProductImageRepository>()
    val useCase = UpdateProductDraftUseCase(
        productRepository = productRepository,
        productPriceRepository = productPriceRepository,
        productImageRepository = productImageRepository,
    )

    beforeEach {
        clearMocks(productRepository, productPriceRepository, productImageRepository)
    }

    Given("DRAFT 상품 업데이트를 요청할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 999L,
                    step = 2,
                    name = "테스트 상품",
                )

                every { productRepository.findById(999L) } returns null

                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("다른 사용자의 상품을 수정하려고 하면") {
            Then("PRODUCT_OWNERSHIP_DENIED 에러가 발생한다") {
                val product = Product(
                    productId = 1L,
                    userId = 100L,
                    status = ProductStatus.DRAFT,
                )

                val command = UpdateProductDraftCommand(
                    userId = 200L,
                    productId = 1L,
                    step = 2,
                    name = "테스트 상품",
                )

                every { productRepository.findById(1L) } returns product

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }

        When("DRAFT가 아닌 상품을 수정하려고 하면") {
            Then("PRODUCT_NOT_DRAFT 에러가 발생한다") {
                val product = Product(
                    productId = 1L,
                    userId = 1L,
                    status = ProductStatus.UNDER_REVIEW,
                )

                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 1L,
                    step = 2,
                    name = "테스트 상품",
                )

                every { productRepository.findById(1L) } returns product

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_DRAFT
            }
        }

        When("정상적으로 기본 정보를 업데이트하면") {
            Then("상품 정보가 업데이트되고 저장된다") {
                val product = Product(
                    productId = 1L,
                    userId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 1,
                )

                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 1L,
                    step = 2,
                    name = "맥북 프로 16인치",
                    description = "2024년형 M3 Max 탑재 모델입니다",
                    categoryCode = "ELECTRONICS",
                    condition = ProductCondition.LIKE_NEW,
                    depositAmount = 500000L,
                )

                every { productRepository.findById(1L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }

                val result = useCase.execute(command)

                result.status shouldBe ProductStatus.DRAFT
                result.currentDraftStep shouldBe 2
                result.name shouldBe "맥북 프로 16인치"
                result.categoryCode shouldBe "ELECTRONICS"
                verify(exactly = 1) { productRepository.save(any()) }
            }
        }

        When("가격 정보를 포함하여 업데이트하면") {
            Then("기존 가격이 삭제되고 새 가격이 저장된다") {
                val product = Product(
                    productId = 2L,
                    userId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 1,
                )

                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 2L,
                    step = 3,
                    name = "캠핑 텐트",
                    prices = listOf(
                        PriceCommand(rentalUnit = RentalUnit.DAILY, priceAmount = 30000L),
                        PriceCommand(rentalUnit = RentalUnit.MONTHLY, priceAmount = 500000L),
                    ),
                )

                every { productRepository.findById(2L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }
                every { productPriceRepository.deleteByProductId(2L) } just Runs
                every { productPriceRepository.saveAll(any()) } answers { firstArg() }

                val result = useCase.execute(command)

                result.currentDraftStep shouldBe 3
                verify(exactly = 1) { productPriceRepository.deleteByProductId(2L) }
                verify(exactly = 1) { productPriceRepository.saveAll(any()) }
            }
        }

        When("이미지 정보를 포함하여 업데이트하면") {
            Then("기존 이미지가 삭제되고 새 이미지가 저장된다") {
                val product = Product(
                    productId = 3L,
                    userId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 2,
                )

                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 3L,
                    step = 4,
                    name = "캠핑 의자",
                    images = listOf(
                        ImageCommand(
                            objectKey = "products/uuid-001.jpg",
                            originalFilename = "front.jpg",
                            sortOrder = 1,
                        ),
                        ImageCommand(
                            objectKey = "products/uuid-002.jpg",
                            originalFilename = "back.jpg",
                            sortOrder = 2,
                        ),
                    ),
                )

                every { productRepository.findById(3L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }
                every { productImageRepository.deleteByProductId(3L) } just Runs
                every { productImageRepository.saveAll(any()) } answers { firstArg() }

                val result = useCase.execute(command)

                result.currentDraftStep shouldBe 4
                verify(exactly = 1) { productImageRepository.deleteByProductId(3L) }
                verify(exactly = 1) { productImageRepository.saveAll(any()) }
            }
        }

        When("가격과 이미지를 모두 포함하여 업데이트하면") {
            Then("가격과 이미지가 모두 교체되고 상품 정보가 업데이트된다") {
                val product = Product(
                    productId = 4L,
                    userId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 1,
                )

                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 4L,
                    step = 5,
                    name = "DSLR 카메라",
                    description = "캐논 EOS R5 풀프레임 미러리스",
                    categoryCode = "ELECTRONICS",
                    condition = ProductCondition.GOOD,
                    depositAmount = 1000000L,
                    prices = listOf(
                        PriceCommand(rentalUnit = RentalUnit.DAILY, priceAmount = 50000L),
                    ),
                    images = listOf(
                        ImageCommand(
                            objectKey = "products/uuid-100.jpg",
                            originalFilename = "camera-front.jpg",
                            sortOrder = 1,
                        ),
                    ),
                )

                every { productRepository.findById(4L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }
                every { productPriceRepository.deleteByProductId(4L) } just Runs
                every { productPriceRepository.saveAll(any()) } answers { firstArg() }
                every { productImageRepository.deleteByProductId(4L) } just Runs
                every { productImageRepository.saveAll(any()) } answers { firstArg() }

                val result = useCase.execute(command)

                result.currentDraftStep shouldBe 5
                result.name shouldBe "DSLR 카메라"
                result.categoryCode shouldBe "ELECTRONICS"
                verify(exactly = 1) { productPriceRepository.deleteByProductId(4L) }
                verify(exactly = 1) { productPriceRepository.saveAll(any()) }
                verify(exactly = 1) { productImageRepository.deleteByProductId(4L) }
                verify(exactly = 1) { productImageRepository.saveAll(any()) }
            }
        }

        When("가격과 이미지가 null이면 기존 데이터를 유지한다") {
            Then("가격/이미지 삭제 및 저장이 호출되지 않는다") {
                val product = Product(
                    productId = 5L,
                    userId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 2,
                )

                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 5L,
                    step = 3,
                    name = "업데이트된 상품명",
                    prices = null,
                    images = null,
                )

                every { productRepository.findById(5L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }

                useCase.execute(command)

                verify(exactly = 0) { productPriceRepository.deleteByProductId(any()) }
                verify(exactly = 0) { productPriceRepository.saveAll(any()) }
                verify(exactly = 0) { productImageRepository.deleteByProductId(any()) }
                verify(exactly = 0) { productImageRepository.saveAll(any()) }
            }
        }
    }
})
