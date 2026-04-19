package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdateProductDraftUseCaseTest : BehaviorSpec({

    val productDomainService = mockk<ProductDomainService>()
    val useCase = UpdateProductDraftUseCase(
        productDomainService = productDomainService,
    )

    beforeEach {
        clearMocks(productDomainService)
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

                every {
                    productDomainService.updateDraft(
                        productId = 999L,
                        userId = 1L,
                        step = 2,
                        name = "테스트 상품",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = null,
                        images = null,
                    )
                } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                    message = "상품을 찾을 수 없습니다 (id=999)",
                )

                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("다른 사용자의 상품을 수정하려고 하면") {
            Then("PRODUCT_OWNERSHIP_DENIED 에러가 발생한다") {
                val command = UpdateProductDraftCommand(
                    userId = 200L,
                    productId = 1L,
                    step = 2,
                    name = "테스트 상품",
                )

                every {
                    productDomainService.updateDraft(
                        productId = 1L,
                        userId = 200L,
                        step = 2,
                        name = "테스트 상품",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = null,
                        images = null,
                    )
                } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                    message = "해당 상품의 소유자가 아닙니다 (productId=1)",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }

        When("DRAFT가 아닌 상품을 수정하려고 하면") {
            Then("PRODUCT_NOT_DRAFT 에러가 발생한다") {
                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 1L,
                    step = 2,
                    name = "테스트 상품",
                )

                every {
                    productDomainService.updateDraft(
                        productId = 1L,
                        userId = 1L,
                        step = 2,
                        name = "테스트 상품",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = null,
                        images = null,
                    )
                } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_NOT_DRAFT,
                    message = "임시저장 상태의 상품만 수정할 수 있습니다",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_DRAFT
            }
        }

        When("정상적으로 기본 정보를 업데이트하면") {
            Then("상품 정보가 업데이트되고 저장된다") {
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

                every {
                    productDomainService.updateDraft(
                        productId = 1L,
                        userId = 1L,
                        step = 2,
                        name = "맥북 프로 16인치",
                        description = "2024년형 M3 Max 탑재 모델입니다",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.LIKE_NEW,
                        depositAmount = 500000L,
                        prices = null,
                        images = null,
                    )
                } returns Product(
                    productId = 1L,
                    userId = 1L,
                    name = "맥북 프로 16인치",
                    categoryCode = "ELECTRONICS",
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 2,
                )

                val result = useCase.execute(command)

                result.status shouldBe ProductStatus.DRAFT
                result.currentDraftStep shouldBe 2
                result.name shouldBe "맥북 프로 16인치"
                result.categoryCode shouldBe "ELECTRONICS"
                verify(exactly = 1) {
                    productDomainService.updateDraft(
                        productId = 1L,
                        userId = 1L,
                        step = 2,
                        name = "맥북 프로 16인치",
                        description = "2024년형 M3 Max 탑재 모델입니다",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.LIKE_NEW,
                        depositAmount = 500000L,
                        prices = null,
                        images = null,
                    )
                }
            }
        }

        When("가격 정보를 포함하여 업데이트하면") {
            Then("기존 가격이 삭제되고 새 가격이 저장된다") {
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

                every {
                    productDomainService.updateDraft(
                        productId = 2L,
                        userId = 1L,
                        step = 3,
                        name = "캠핑 텐트",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = any(),
                        images = null,
                    )
                } returns Product(
                    productId = 2L,
                    userId = 1L,
                    name = "캠핑 텐트",
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 3,
                )

                val result = useCase.execute(command)

                result.currentDraftStep shouldBe 3
                verify(exactly = 1) {
                    productDomainService.updateDraft(
                        productId = 2L,
                        userId = 1L,
                        step = 3,
                        name = "캠핑 텐트",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = any(),
                        images = null,
                    )
                }
            }
        }

        When("이미지 정보를 포함하여 업데이트하면") {
            Then("기존 이미지가 삭제되고 새 이미지가 저장된다") {
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

                every {
                    productDomainService.updateDraft(
                        productId = 3L,
                        userId = 1L,
                        step = 4,
                        name = "캠핑 의자",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = null,
                        images = any(),
                    )
                } returns Product(
                    productId = 3L,
                    userId = 1L,
                    name = "캠핑 의자",
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 4,
                )

                val result = useCase.execute(command)

                result.currentDraftStep shouldBe 4
                verify(exactly = 1) {
                    productDomainService.updateDraft(
                        productId = 3L,
                        userId = 1L,
                        step = 4,
                        name = "캠핑 의자",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = null,
                        images = any(),
                    )
                }
            }
        }

        When("가격과 이미지를 모두 포함하여 업데이트하면") {
            Then("가격과 이미지가 모두 교체되고 상품 정보가 업데이트된다") {
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

                every {
                    productDomainService.updateDraft(
                        productId = 4L,
                        userId = 1L,
                        step = 5,
                        name = "DSLR 카메라",
                        description = "캐논 EOS R5 풀프레임 미러리스",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.GOOD,
                        depositAmount = 1000000L,
                        prices = any(),
                        images = any(),
                    )
                } returns Product(
                    productId = 4L,
                    userId = 1L,
                    name = "DSLR 카메라",
                    categoryCode = "ELECTRONICS",
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 5,
                )

                val result = useCase.execute(command)

                result.currentDraftStep shouldBe 5
                result.name shouldBe "DSLR 카메라"
                result.categoryCode shouldBe "ELECTRONICS"
                verify(exactly = 1) {
                    productDomainService.updateDraft(
                        productId = 4L,
                        userId = 1L,
                        step = 5,
                        name = "DSLR 카메라",
                        description = "캐논 EOS R5 풀프레임 미러리스",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.GOOD,
                        depositAmount = 1000000L,
                        prices = any(),
                        images = any(),
                    )
                }
            }
        }

        When("가격과 이미지가 null이면 기존 데이터를 유지한다") {
            Then("가격/이미지 삭제 및 저장이 호출되지 않는다") {
                val command = UpdateProductDraftCommand(
                    userId = 1L,
                    productId = 5L,
                    step = 3,
                    name = "업데이트된 상품명",
                    prices = null,
                    images = null,
                )

                every {
                    productDomainService.updateDraft(
                        productId = 5L,
                        userId = 1L,
                        step = 3,
                        name = "업데이트된 상품명",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = null,
                        images = null,
                    )
                } returns Product(
                    productId = 5L,
                    userId = 1L,
                    name = "업데이트된 상품명",
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 3,
                )

                useCase.execute(command)

                verify(exactly = 1) {
                    productDomainService.updateDraft(
                        productId = 5L,
                        userId = 1L,
                        step = 3,
                        name = "업데이트된 상품명",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                        prices = null,
                        images = null,
                    )
                }
            }
        }
    }
})
