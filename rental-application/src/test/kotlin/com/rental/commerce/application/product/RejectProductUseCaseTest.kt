package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.event.ProductRejectedEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RejectProductUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val useCase = RejectProductUseCase(productRepository)

    beforeEach {
        clearMocks(productRepository)
    }

    Given("상품 반려 요청을 할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val command = RejectProductCommand(productId = 999L, reason = "반려 사유")

                every { productRepository.findById(999L) } returns null

                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("UNDER_REVIEW 상태가 아닌 상품을 반려하려고 하면") {
            Then("PRODUCT_NOT_UNDER_REVIEW 에러가 발생한다") {
                val product = Product(
                    productId = 1L,
                    userId = 1L,
                    status = ProductStatus.DRAFT,
                )

                val command = RejectProductCommand(productId = 1L, reason = "반려 사유")

                every { productRepository.findById(1L) } returns product

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_UNDER_REVIEW
            }
        }

        When("UNDER_REVIEW 상태의 상품을 반려하면") {
            Then("상품 상태가 REJECTED로 변경되고 저장된다") {
                val product = Product(
                    productId = 2L,
                    userId = 1L,
                    status = ProductStatus.UNDER_REVIEW,
                )

                val command = RejectProductCommand(productId = 2L, reason = "상품 설명이 부족합니다")

                every { productRepository.findById(2L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }

                useCase.execute(command)

                product.status shouldBe ProductStatus.REJECTED
                product.rejectReason shouldBe "상품 설명이 부족합니다"
                verify(exactly = 1) { productRepository.save(product) }
            }
        }

        When("UNDER_REVIEW 상태의 상품을 반려하면 이벤트가 발행된다") {
            Then("ProductRejectedEvent가 발행되고 반려 사유가 포함된다") {
                val product = Product(
                    productId = 3L,
                    userId = 20L,
                    status = ProductStatus.UNDER_REVIEW,
                )

                val command = RejectProductCommand(productId = 3L, reason = "이미지가 불명확합니다")

                every { productRepository.findById(3L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }

                useCase.execute(command)

                val events = product.pullEvents()
                events.size shouldBe 1
                val event = events.first()
                event.shouldBeInstanceOf<ProductRejectedEvent>()
                (event as ProductRejectedEvent).reason shouldBe "이미지가 불명확합니다"
                event.productId shouldBe 3L
                event.userId shouldBe 20L
            }
        }
    }
})
