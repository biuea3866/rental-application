package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.ApproveProductUseCase
import com.rental.commerce.application.product.RejectProductUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AdminProductApiControllerTest : BehaviorSpec({

    val approveProductUseCase = mockk<ApproveProductUseCase>()
    val rejectProductUseCase = mockk<RejectProductUseCase>()

    val controller = AdminProductApiController(
        approveProductUseCase = approveProductUseCase,
        rejectProductUseCase = rejectProductUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(approveProductUseCase, rejectProductUseCase)
    }

    Given("PATCH /api/admin/products/{productId}/approve") {

        When("UNDER_REVIEW 상태의 상품을 승인하면") {
            Then("200 OK가 반환된다") {
                every { approveProductUseCase.execute(any()) } just Runs

                val result = mockMvc.patch("/api/admin/products/1/approve")

                result.andExpect {
                    status { isOk() }
                }
                verify(exactly = 1) { approveProductUseCase.execute(any()) }
            }
        }

        When("존재하지 않는 상품을 승인하려고 하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                every { approveProductUseCase.execute(any()) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                )

                val result = mockMvc.patch("/api/admin/products/999/approve")

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }

        When("UNDER_REVIEW 상태가 아닌 상품을 승인하려고 하면") {
            Then("400 PRODUCT_NOT_UNDER_REVIEW 에러가 반환된다") {
                every { approveProductUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_NOT_UNDER_REVIEW,
                )

                val result = mockMvc.patch("/api/admin/products/1/approve")

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("PRODUCT_NOT_UNDER_REVIEW") }
                }
            }
        }
    }

    Given("PATCH /api/admin/products/{productId}/reject") {

        When("UNDER_REVIEW 상태의 상품을 반려하면") {
            Then("200 OK가 반환된다") {
                every { rejectProductUseCase.execute(any()) } just Runs

                val result = mockMvc.patch("/api/admin/products/1/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": "상품 설명이 부족합니다"}"""
                }

                result.andExpect {
                    status { isOk() }
                }
                verify(exactly = 1) { rejectProductUseCase.execute(any()) }
            }
        }

        When("reason 없이 반려 요청을 하면") {
            Then("400 Validation Error가 반환된다") {
                val result = mockMvc.patch("/api/admin/products/1/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": ""}"""
                }

                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("존재하지 않는 상품을 반려하려고 하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                every { rejectProductUseCase.execute(any()) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                )

                val result = mockMvc.patch("/api/admin/products/999/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": "반려 사유"}"""
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }

        When("UNDER_REVIEW 상태가 아닌 상품을 반려하려고 하면") {
            Then("400 PRODUCT_NOT_UNDER_REVIEW 에러가 반환된다") {
                every { rejectProductUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_NOT_UNDER_REVIEW,
                )

                val result = mockMvc.patch("/api/admin/products/1/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": "반려 사유"}"""
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("PRODUCT_NOT_UNDER_REVIEW") }
                }
            }
        }
    }
})
