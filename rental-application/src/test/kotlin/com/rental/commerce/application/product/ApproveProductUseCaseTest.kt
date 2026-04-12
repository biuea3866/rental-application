package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class ApproveProductUseCaseTest : BehaviorSpec({

    val productDomainService = mockk<ProductDomainService>()
    val useCase = ApproveProductUseCase(productDomainService)

    beforeEach {
        clearMocks(productDomainService)
    }

    Given("상품 승인 요청을 할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val command = ApproveProductCommand(productId = 999L)

                every { productDomainService.approve(999L) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                    message = "상품을 찾을 수 없습니다 (id=999)",
                )

                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("UNDER_REVIEW 상태가 아닌 상품을 승인하려고 하면") {
            Then("PRODUCT_NOT_UNDER_REVIEW 에러가 발생한다") {
                val command = ApproveProductCommand(productId = 1L)

                every { productDomainService.approve(1L) } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_NOT_UNDER_REVIEW,
                    message = "검수 중인 상품만 승인할 수 있습니다",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_UNDER_REVIEW
            }
        }

        When("UNDER_REVIEW 상태의 상품을 승인하면") {
            Then("productDomainService.approve()가 호출된다") {
                val command = ApproveProductCommand(productId = 2L)

                every { productDomainService.approve(2L) } just runs

                useCase.execute(command)

                verify(exactly = 1) { productDomainService.approve(2L) }
            }
        }
    }
})
