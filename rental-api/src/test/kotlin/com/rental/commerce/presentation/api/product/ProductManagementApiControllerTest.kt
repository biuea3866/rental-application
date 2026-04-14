package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.DeleteProductUseCase
import com.rental.commerce.application.product.GetMyProductsUseCase
import com.rental.commerce.application.product.MyProductSummaryResponse
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZonedDateTime

class ProductManagementApiControllerTest : BehaviorSpec({

    val getMyProductsUseCase = mockk<GetMyProductsUseCase>()
    val deleteProductUseCase = mockk<DeleteProductUseCase>()
    val controller = ProductManagementApiController(
        getMyProductsUseCase = getMyProductsUseCase,
        deleteProductUseCase = deleteProductUseCase,
    )
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(getMyProductsUseCase, deleteProductUseCase)
    }

    Given("GET /api/v1/my-products") {

        When("정상적으로 내 상품 목록을 조회하면") {
            Then("200 OK와 페이징된 상품 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
                val products = listOf(
                    MyProductSummaryResponse(
                        id = 1L,
                        name = "맥북 프로",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.LIKE_NEW,
                        status = ProductStatus.DRAFT,
                        depositAmount = 500_000L,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    MyProductSummaryResponse(
                        id = 2L,
                        name = "아이패드",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.GOOD,
                        status = ProductStatus.AVAILABLE,
                        depositAmount = 200_000L,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )

                every {
                    getMyProductsUseCase.execute(userId = 1L, page = 0, size = 20)
                } returns PageImpl(products, pageable, 2L)

                val result = mockMvc.get("/api/v1/my-products") {
                    param("page", "0")
                    param("size", "20")
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(2) }
                    jsonPath("$.content[0].id") { value(1) }
                    jsonPath("$.content[0].name") { value("맥북 프로") }
                    jsonPath("$.content[0].status") { value("DRAFT") }
                    jsonPath("$.content[1].id") { value(2) }
                    jsonPath("$.content[1].name") { value("아이패드") }
                    jsonPath("$.content[1].status") { value("AVAILABLE") }
                    jsonPath("$.totalElements") { value(2) }
                    jsonPath("$.totalPages") { value(1) }
                    jsonPath("$.number") { value(0) }
                }
            }
        }

        When("상품이 없는 사용자가 조회하면") {
            Then("200 OK와 빈 목록이 반환된다") {
                val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

                every {
                    getMyProductsUseCase.execute(userId = 2L, page = 0, size = 20)
                } returns PageImpl(emptyList(), pageable, 0L)

                val result = mockMvc.get("/api/v1/my-products") {
                    param("page", "0")
                    param("size", "20")
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "2")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(0) }
                    jsonPath("$.totalElements") { value(0) }
                }
            }
        }

        When("page와 size 파라미터 없이 조회하면") {
            Then("기본값(page=0, size=20)으로 조회된다") {
                val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

                every {
                    getMyProductsUseCase.execute(userId = 1L, page = 0, size = 20)
                } returns PageImpl(emptyList(), pageable, 0L)

                val result = mockMvc.get("/api/v1/my-products") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify { getMyProductsUseCase.execute(userId = 1L, page = 0, size = 20) }
            }
        }
    }

    Given("DELETE /api/v1/my-products/{productId}") {

        When("DRAFT 상품을 정상적으로 삭제하면") {
            Then("204 No Content가 반환된다") {
                justRun { deleteProductUseCase.execute(userId = 1L, productId = 1L) }

                val result = mockMvc.delete("/api/v1/my-products/1") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNoContent() }
                }

                verify(exactly = 1) { deleteProductUseCase.execute(userId = 1L, productId = 1L) }
            }
        }

        When("존재하지 않는 상품을 삭제하려고 하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                every {
                    deleteProductUseCase.execute(userId = 1L, productId = 999L)
                } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                )

                val result = mockMvc.delete("/api/v1/my-products/999") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }

        When("권한이 없는 상품을 삭제하려고 하면") {
            Then("403 PRODUCT_OWNERSHIP_DENIED 에러가 반환된다") {
                every {
                    deleteProductUseCase.execute(userId = 1L, productId = 1L)
                } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                )

                val result = mockMvc.delete("/api/v1/my-products/1") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("PRODUCT_OWNERSHIP_DENIED") }
                }
            }
        }

        When("삭제할 수 없는 상태의 상품을 삭제하려고 하면") {
            Then("400 PRODUCT_NOT_DELETABLE 에러가 반환된다") {
                every {
                    deleteProductUseCase.execute(userId = 1L, productId = 1L)
                } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_NOT_DELETABLE,
                )

                val result = mockMvc.delete("/api/v1/my-products/1") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("PRODUCT_NOT_DELETABLE") }
                }
            }
        }
    }
})
