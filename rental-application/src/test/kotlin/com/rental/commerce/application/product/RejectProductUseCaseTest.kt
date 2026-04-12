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

class RejectProductUseCaseTest : BehaviorSpec({

    val productDomainService = mockk<ProductDomainService>()
    val useCase = RejectProductUseCase(productDomainService)

    beforeEach {
        clearMocks(productDomainService)
    }

    Given("상품 반려 요청을 할 때") {

        When("존재하지 않는 상품 ID로 요청하면") {
            Then("PRODUCT_NOT_FOUND 에러가 발생한다") {
                val command = RejectProductCommand(productId = 999L, reason = "반려 사유")

                every { productDomainService.reject(999L, "반려 사유") } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                    message = "상품을 찾을 수 없습니다 (id=999)",
                )

                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_FOUND
            }
        }

        When("UNDER_REVIEW 상태가 아닌 상품을 반려하려고 하면") {
            Then("PRODUCT_NOT_UNDER_REVIEW 에러가 발생한다") {
                val command = RejectProductCommand(productId = 1L, reason = "반려 사유")

                every { productDomainService.reject(1L, "반려 사유") } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_NOT_UNDER_REVIEW,
                    message = "검수 중인 상품만 반려할 수 있습니다",
                )

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_UNDER_REVIEW
            }
        }

        When("UNDER_REVIEW 상태의 상품을 반려하면") {
            Then("productDomainService.reject()가 호출된다") {
                val command = RejectProductCommand(productId = 2L, reason = "상품 설명이 부족합니다")

                every { productDomainService.reject(2L, "상품 설명이 부족합니다") } just runs

                useCase.execute(command)

                verify(exactly = 1) { productDomainService.reject(2L, "상품 설명이 부족합니다") }
            }
        }
    }
})
