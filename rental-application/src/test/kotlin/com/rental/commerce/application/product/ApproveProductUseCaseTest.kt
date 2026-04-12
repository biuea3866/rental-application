package com.rental.commerce.application.product

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ApproveProductUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val useCase = ApproveProductUseCase(productRepository)

    beforeEach {
        clearMocks(productRepository)
    }

    Given("상품 승인 요청을 할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val command = ApproveProductCommand(productId = 999L)

                every { productRepository.findById(999L) } returns null

                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("UNDER_REVIEW 상태가 아닌 상품을 승인하려고 하면") {
            Then("PRODUCT_NOT_UNDER_REVIEW 에러가 발생한다") {
                val product = Product(
                    productId = 1L,
                    userId = 1L,
                    status = ProductStatus.DRAFT,
                )

                val command = ApproveProductCommand(productId = 1L)

                every { productRepository.findById(1L) } returns product

                val exception = shouldThrow<com.rental.commerce.domain.common.BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_UNDER_REVIEW
            }
        }

        When("UNDER_REVIEW 상태의 상품을 승인하면") {
            Then("상품 상태가 APPROVED로 변경되고 저장된다") {
                val product = Product(
                    productId = 2L,
                    userId = 1L,
                    status = ProductStatus.UNDER_REVIEW,
                )

                val command = ApproveProductCommand(productId = 2L)

                every { productRepository.findById(2L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }

                useCase.execute(command)

                product.status shouldBe ProductStatus.APPROVED
                verify(exactly = 1) { productRepository.save(product) }
            }
        }

        When("UNDER_REVIEW 상태의 상품을 승인하면 이벤트가 발행된다") {
            Then("ProductApprovedEvent가 발행된다") {
                val product = Product(
                    productId = 3L,
                    userId = 10L,
                    status = ProductStatus.UNDER_REVIEW,
                )

                val command = ApproveProductCommand(productId = 3L)

                every { productRepository.findById(3L) } returns product
                every { productRepository.save(any()) } answers { firstArg() }

                useCase.execute(command)

                val events = product.pullEvents()
                events.size shouldBe 1
                events.first() shouldBe com.rental.commerce.domain.product.event.ProductApprovedEvent(
                    productId = 3L,
                    userId = 10L,
                    occurredAt = events.first().occurredAt,
                )
            }
        }
    }
})
