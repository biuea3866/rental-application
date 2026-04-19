package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.product.ProductStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SubmitProductUseCaseTest : BehaviorSpec({

    val productDomainService = mockk<ProductDomainService>()
    val useCase = SubmitProductUseCase(productDomainService)

    Given("상품 제출(DRAFT→UNDER_REVIEW)을 요청할 때") {

        When("모든 필수 필드가 입력된 DRAFT 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 1L)

            every {
                productDomainService.submit(productId = 1L, userId = 100L)
            } returns Product(
                productId = 1L,
                userId = 100L,
                name = "맥북 프로 16인치",
                description = "2024년형 M3 Max 맥북 프로",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 500000L,
                status = ProductStatus.UNDER_REVIEW,
            )

            Then("상품 상태가 UNDER_REVIEW로 변경되고 올바른 응답이 반환된다") {
                val result = useCase.execute(command)

                result.status shouldBe ProductStatus.UNDER_REVIEW
                result.productId shouldBe 1L
                result.name shouldBe "맥북 프로 16인치"
                verify(exactly = 1) { productDomainService.submit(productId = 1L, userId = 100L) }
            }
        }

        When("존재하지 않는 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 999L)

            every {
                productDomainService.submit(productId = 999L, userId = 100L)
            } throws ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=999)",
            )

            Then("PRODUCT_NOT_FOUND 예외가 발생한다") {
                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("소유자가 아닌 사용자가 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 999L, productId = 2L)

            every {
                productDomainService.submit(productId = 2L, userId = 999L)
            } throws BusinessException(
                errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                message = "해당 상품의 소유자가 아닙니다 (productId=2)",
            )

            Then("PRODUCT_OWNERSHIP_DENIED 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }

        When("DRAFT가 아닌 상태의 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 3L)

            every {
                productDomainService.submit(productId = 3L, userId = 100L)
            } throws InvalidStateTransitionException("UNDER_REVIEW에서 UNDER_REVIEW(으)로 전이할 수 없습니다")

            Then("InvalidStateTransitionException 예외가 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }

        When("상품명이 없는 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 4L)

            every {
                productDomainService.submit(productId = 4L, userId = 100L)
            } throws BusinessException(ErrorCode.INVALID_INPUT, "상품명은 필수입니다")

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("상품 설명이 없는 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 5L)

            every {
                productDomainService.submit(productId = 5L, userId = 100L)
            } throws BusinessException(ErrorCode.INVALID_INPUT, "상품 설명은 필수입니다")

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("카테고리 코드가 없는 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 6L)

            every {
                productDomainService.submit(productId = 6L, userId = 100L)
            } throws BusinessException(ErrorCode.INVALID_INPUT, "카테고리 코드는 필수입니다")

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("보증금이 없는 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 7L)

            every {
                productDomainService.submit(productId = 7L, userId = 100L)
            } throws BusinessException(ErrorCode.INVALID_INPUT, "보증금은 필수입니다")

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("상품 상태(condition)가 없는 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 8L)

            every {
                productDomainService.submit(productId = 8L, userId = 100L)
            } throws BusinessException(ErrorCode.INVALID_INPUT, "상품 상태는 필수입니다")

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }
    }
})
