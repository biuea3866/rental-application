package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.CreateProductDraftCommand
import com.rental.commerce.application.product.CreateProductDraftUseCase
import com.rental.commerce.application.product.GetProductDraftUseCase
import com.rental.commerce.application.product.ProductDraftDetailResponse
import com.rental.commerce.application.product.ProductDraftResponse
import com.rental.commerce.application.product.UpdateProductDraftUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ProductApiControllerTest : BehaviorSpec({

    val createProductDraftUseCase = mockk<CreateProductDraftUseCase>()
    val updateProductDraftUseCase = mockk<UpdateProductDraftUseCase>()
    val getProductDraftUseCase = mockk<GetProductDraftUseCase>()
    val controller = ProductApiController(
        createProductDraftUseCase = createProductDraftUseCase,
        updateProductDraftUseCase = updateProductDraftUseCase,
        getProductDraftUseCase = getProductDraftUseCase,
    )
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    fun setAuthentication(userId: Long = 1L) {
        val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication
    }

    beforeEach {
        clearMocks(createProductDraftUseCase, updateProductDraftUseCase, getProductDraftUseCase)
    }

    afterEach {
        SecurityContextHolder.clearContext()
    }

    Given("POST /api/v1/products/drafts") {

        When("정상적인 DRAFT 생성 요청을 보내면") {
            Then("201 Created와 상품 정보가 반환된다") {
                setAuthentication()

                val response = ProductDraftResponse(
                    productId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 1,
                    name = "맥북 프로",
                    categoryCode = "ELECTRONICS",
                )

                every {
                    createProductDraftUseCase.execute(
                        CreateProductDraftCommand(
                            userId = 1L,
                            name = "맥북 프로",
                            categoryCode = "ELECTRONICS",
                        )
                    )
                } returns response

                val result = mockMvc.post("/api/v1/products/drafts") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"맥북 프로","categoryCode":"ELECTRONICS"}"""
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.productId") { value(1) }
                    jsonPath("$.status") { value("DRAFT") }
                    jsonPath("$.currentDraftStep") { value(1) }
                    jsonPath("$.name") { value("맥북 프로") }
                    jsonPath("$.categoryCode") { value("ELECTRONICS") }
                }
            }
        }

        When("빈 요청 바디로 DRAFT 생성 요청을 보내면") {
            Then("201 Created가 반환된다") {
                setAuthentication()

                val response = ProductDraftResponse(
                    productId = 2L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 1,
                    name = null,
                    categoryCode = null,
                )

                every {
                    createProductDraftUseCase.execute(
                        CreateProductDraftCommand(userId = 1L)
                    )
                } returns response

                val result = mockMvc.post("/api/v1/products/drafts") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{}"""
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.productId") { value(2) }
                    jsonPath("$.status") { value("DRAFT") }
                }
            }
        }
    }

    Given("PATCH /api/v1/products/drafts/{productId}") {

        When("정상적인 DRAFT 업데이트 요청을 보내면") {
            Then("200 OK와 업데이트된 상품 정보가 반환된다") {
                setAuthentication()

                val response = ProductDraftResponse(
                    productId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 2,
                    name = "맥북 프로 16인치",
                    categoryCode = "ELECTRONICS",
                )

                every {
                    updateProductDraftUseCase.execute(any())
                } returns response

                val result = mockMvc.patch("/api/v1/products/drafts/1") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{
                        "step": 2,
                        "name": "맥북 프로 16인치",
                        "description": "2024년형 M3 Max",
                        "categoryCode": "ELECTRONICS",
                        "condition": "LIKE_NEW",
                        "depositAmount": 500000
                    }"""
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.productId") { value(1) }
                    jsonPath("$.currentDraftStep") { value(2) }
                    jsonPath("$.name") { value("맥북 프로 16인치") }
                }
            }
        }

        When("step 값이 1 미만인 요청을 보내면") {
            Then("400 Validation Error가 반환된다") {
                setAuthentication()

                val result = mockMvc.patch("/api/v1/products/drafts/1") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"step": 0}"""
                }

                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("step 값이 10 초과인 요청을 보내면") {
            Then("400 Validation Error가 반환된다") {
                setAuthentication()

                val result = mockMvc.patch("/api/v1/products/drafts/1") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"step": 11}"""
                }

                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("존재하지 않는 상품을 업데이트하려고 하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                setAuthentication()

                every {
                    updateProductDraftUseCase.execute(any())
                } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                )

                val result = mockMvc.patch("/api/v1/products/drafts/999") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"step": 2, "name": "테스트"}"""
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }

        When("권한이 없는 상품을 업데이트하려고 하면") {
            Then("403 PRODUCT_OWNERSHIP_DENIED 에러가 반환된다") {
                setAuthentication()

                every {
                    updateProductDraftUseCase.execute(any())
                } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                )

                val result = mockMvc.patch("/api/v1/products/drafts/1") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"step": 2, "name": "테스트"}"""
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("PRODUCT_OWNERSHIP_DENIED") }
                }
            }
        }
    }

    Given("GET /api/v1/products/drafts/{productId}") {

        When("정상적으로 DRAFT 상품 상세를 조회하면") {
            Then("200 OK와 상세 정보가 반환된다") {
                setAuthentication()

                val response = ProductDraftDetailResponse(
                    productId = 1L,
                    status = ProductStatus.DRAFT,
                    currentDraftStep = 3,
                    name = "맥북 프로",
                    description = "최신형 맥북 프로입니다",
                    categoryCode = "ELECTRONICS",
                    condition = ProductCondition.LIKE_NEW,
                    depositAmount = 500000L,
                    prices = listOf(
                        ProductDraftDetailResponse.PriceResponse(
                            productPriceId = 10L,
                            rentalUnit = RentalUnit.DAILY,
                            priceAmount = 30000L,
                        ),
                    ),
                    images = listOf(
                        ProductDraftDetailResponse.ImageResponse(
                            productImageId = 20L,
                            objectKey = "products/uuid-001.jpg",
                            originalFilename = "front.jpg",
                            sortOrder = 1,
                        ),
                    ),
                )

                every {
                    getProductDraftUseCase.execute(userId = 1L, productId = 1L)
                } returns response

                val result = mockMvc.get("/api/v1/products/drafts/1")

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.productId") { value(1) }
                    jsonPath("$.name") { value("맥북 프로") }
                    jsonPath("$.description") { value("최신형 맥북 프로입니다") }
                    jsonPath("$.categoryCode") { value("ELECTRONICS") }
                    jsonPath("$.condition") { value("LIKE_NEW") }
                    jsonPath("$.depositAmount") { value(500000) }
                    jsonPath("$.prices.length()") { value(1) }
                    jsonPath("$.prices[0].rentalUnit") { value("DAILY") }
                    jsonPath("$.prices[0].priceAmount") { value(30000) }
                    jsonPath("$.images.length()") { value(1) }
                    jsonPath("$.images[0].objectKey") { value("products/uuid-001.jpg") }
                }
            }
        }

        When("존재하지 않는 상품을 조회하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                setAuthentication()

                every {
                    getProductDraftUseCase.execute(userId = 1L, productId = 999L)
                } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                )

                val result = mockMvc.get("/api/v1/products/drafts/999")

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }
    }
})
