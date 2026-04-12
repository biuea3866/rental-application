package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SubmitProductUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val useCase = SubmitProductUseCase(productRepository)

    Given("상품 제출(DRAFT→UNDER_REVIEW)을 요청할 때") {

        When("모든 필수 필드가 입력된 DRAFT 상품을 제출하면") {
            val product = Product(
                productId = 1L,
                userId = 100L,
                name = "맥북 프로 16인치",
                description = "2024년형 M3 Max 맥북 프로",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 500000L,
                status = ProductStatus.DRAFT,
                currentDraftStep = 5,
            )

            val command = SubmitProductCommand(userId = 100L, productId = 1L)

            every { productRepository.findById(1L) } returns product
            every { productRepository.save(any()) } answers { firstArg() }

            val result = useCase.execute(command)

            Then("상품 상태가 UNDER_REVIEW로 변경된다") {
                result.status shouldBe ProductStatus.UNDER_REVIEW
            }

            Then("응답에 올바른 productId가 포함된다") {
                result.productId shouldBe 1L
            }

            Then("응답에 올바른 상품명이 포함된다") {
                result.name shouldBe "맥북 프로 16인치"
            }

            Then("ProductRepository.save가 정확히 한 번 호출된다") {
                verify(exactly = 1) { productRepository.save(any()) }
            }
        }

        When("존재하지 않는 상품을 제출하면") {
            val command = SubmitProductCommand(userId = 100L, productId = 999L)

            every { productRepository.findById(999L) } returns null

            Then("PRODUCT_NOT_FOUND 예외가 발생한다") {
                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("소유자가 아닌 사용자가 상품을 제출하면") {
            val product = Product(
                productId = 2L,
                userId = 100L,
                name = "맥북 프로",
                description = "설명",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 500000L,
                status = ProductStatus.DRAFT,
            )

            val command = SubmitProductCommand(userId = 999L, productId = 2L)

            every { productRepository.findById(2L) } returns product

            Then("PRODUCT_OWNERSHIP_DENIED 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_OWNERSHIP_DENIED
            }
        }

        When("DRAFT가 아닌 상태의 상품을 제출하면") {
            val product = Product(
                productId = 3L,
                userId = 100L,
                name = "맥북 프로",
                description = "설명",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 500000L,
                status = ProductStatus.UNDER_REVIEW,
            )

            val command = SubmitProductCommand(userId = 100L, productId = 3L)

            every { productRepository.findById(3L) } returns product

            Then("InvalidStateTransitionException 예외가 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command)
                }
            }
        }

        When("상품명이 없는 상품을 제출하면") {
            val product = Product(
                productId = 4L,
                userId = 100L,
                name = null,
                description = "설명",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 500000L,
                status = ProductStatus.DRAFT,
            )

            val command = SubmitProductCommand(userId = 100L, productId = 4L)

            every { productRepository.findById(4L) } returns product

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("상품 설명이 없는 상품을 제출하면") {
            val product = Product(
                productId = 5L,
                userId = 100L,
                name = "맥북 프로",
                description = null,
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 500000L,
                status = ProductStatus.DRAFT,
            )

            val command = SubmitProductCommand(userId = 100L, productId = 5L)

            every { productRepository.findById(5L) } returns product

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("카테고리 코드가 없는 상품을 제출하면") {
            val product = Product(
                productId = 6L,
                userId = 100L,
                name = "맥북 프로",
                description = "설명",
                categoryCode = null,
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 500000L,
                status = ProductStatus.DRAFT,
            )

            val command = SubmitProductCommand(userId = 100L, productId = 6L)

            every { productRepository.findById(6L) } returns product

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("보증금이 없는 상품을 제출하면") {
            val product = Product(
                productId = 7L,
                userId = 100L,
                name = "맥북 프로",
                description = "설명",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = null,
                status = ProductStatus.DRAFT,
            )

            val command = SubmitProductCommand(userId = 100L, productId = 7L)

            every { productRepository.findById(7L) } returns product

            Then("INVALID_INPUT 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }
    }
})
