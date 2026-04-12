package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
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

class DeleteProductUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val useCase = DeleteProductUseCase(
        productRepository = productRepository,
    )

    Given("상품 삭제를 요청할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            every { productRepository.findById(999L) } returns null

            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(userId = 1L, productId = 999L)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("다른 사용자의 상품을 삭제하려고 하면") {
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

        When("DRAFT 상태의 본인 상품을 삭제하면") {
            val product = Product(
                productId = 1L,
                userId = 1L,
                name = "맥북 프로",
                status = ProductStatus.DRAFT,
            )

            every { productRepository.findById(1L) } returns product
            every { productRepository.save(any()) } answers { firstArg() }

            useCase.execute(userId = 1L, productId = 1L)

            Then("상태가 DELETED로 변경된다") {
                product.status shouldBe ProductStatus.DELETED
            }

            Then("저장소에 저장된다") {
                verify(exactly = 1) { productRepository.save(product) }
            }
        }

        When("REJECTED 상태의 본인 상품을 삭제하면") {
            val product = Product(
                productId = 2L,
                userId = 1L,
                name = "아이패드",
                categoryCode = "ELECTRONICS",
                description = "테스트 설명",
                condition = ProductCondition.GOOD,
                depositAmount = 200_000L,
                status = ProductStatus.DRAFT,
            )
            // DRAFT -> UNDER_REVIEW -> REJECTED
            product.submit()
            product.reject("품질 미달")
            product.pullEvents()

            every { productRepository.findById(2L) } returns product
            every { productRepository.save(any()) } answers { firstArg() }

            useCase.execute(userId = 1L, productId = 2L)

            Then("상태가 DELETED로 변경된다") {
                product.status shouldBe ProductStatus.DELETED
            }

            Then("저장소에 저장된다") {
                verify(exactly = 1) { productRepository.save(product) }
            }
        }

        When("UNDER_REVIEW 상태의 상품을 삭제하려고 하면") {
            val product = Product(
                productId = 3L,
                userId = 1L,
                name = "카메라",
                categoryCode = "ELECTRONICS",
                description = "테스트 설명",
                condition = ProductCondition.GOOD,
                depositAmount = 100_000L,
                status = ProductStatus.DRAFT,
            )
            product.submit()

            every { productRepository.findById(3L) } returns product

            Then("PRODUCT_NOT_DELETABLE 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(userId = 1L, productId = 3L)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_DELETABLE
            }
        }

        When("AVAILABLE 상태의 상품을 삭제하려고 하면") {
            val product = Product(
                productId = 4L,
                userId = 1L,
                name = "자전거",
                categoryCode = "SPORTS",
                description = "테스트 설명",
                condition = ProductCondition.GOOD,
                depositAmount = 150_000L,
                status = ProductStatus.DRAFT,
            )
            product.submit()
            product.approve()
            product.pullEvents()
            product.makeAvailable()

            every { productRepository.findById(4L) } returns product

            Then("PRODUCT_NOT_DELETABLE 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(userId = 1L, productId = 4L)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_DELETABLE
            }
        }
    }
})
