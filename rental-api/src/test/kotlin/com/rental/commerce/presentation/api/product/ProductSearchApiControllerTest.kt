package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.GetProductDetailUseCase
import com.rental.commerce.application.product.ProductDetailResponse
import com.rental.commerce.application.product.ProductSummaryResponse
import com.rental.commerce.application.product.SearchProductCommand
import com.rental.commerce.application.product.SearchProductUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductSortBy
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import com.rental.commerce.domain.product.SortDirection
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZonedDateTime

class ProductSearchApiControllerTest : BehaviorSpec({

    val searchProductUseCase = mockk<SearchProductUseCase>()
    val getProductDetailUseCase = mockk<GetProductDetailUseCase>()
    val controller = ProductSearchApiController(
        searchProductUseCase = searchProductUseCase,
        getProductDetailUseCase = getProductDetailUseCase,
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
        clearMocks(searchProductUseCase, getProductDetailUseCase)
    }

    afterEach {
        SecurityContextHolder.clearContext()
    }

    Given("GET /api/v1/products") {

        When("기본 파라미터로 검색하면") {
            Then("200 OK와 상품 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val summaries = listOf(
                    ProductSummaryResponse(
                        id = 1L,
                        name = "맥북 프로",
                        categoryCode = "ELECTRONICS",
                        status = ProductStatus.AVAILABLE,
                        depositAmount = 500000L,
                        thumbnailUrl = "products/thumb-001.jpg",
                        createdAt = now,
                    ),
                    ProductSummaryResponse(
                        id = 2L,
                        name = "아이패드 에어",
                        categoryCode = "ELECTRONICS",
                        status = ProductStatus.AVAILABLE,
                        depositAmount = 200000L,
                        thumbnailUrl = null,
                        createdAt = now,
                    ),
                )
                val page = PageImpl(summaries, PageRequest.of(0, 20), 2L)

                every { searchProductUseCase.execute(any()) } returns page

                val result = mockMvc.get("/api/v1/products")

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(2) }
                    jsonPath("$.content[0].id") { value(1) }
                    jsonPath("$.content[0].name") { value("맥북 프로") }
                    jsonPath("$.content[0].thumbnailUrl") { value("products/thumb-001.jpg") }
                    jsonPath("$.content[1].id") { value(2) }
                    jsonPath("$.totalElements") { value(2) }
                }

                verify {
                    searchProductUseCase.execute(
                        SearchProductCommand(
                            keyword = null,
                            categoryCode = null,
                            status = ProductStatus.AVAILABLE,
                            minPrice = null,
                            maxPrice = null,
                            rentalUnit = null,
                            page = 0,
                            size = 20,
                            sortBy = ProductSortBy.CREATED_AT,
                            sortDirection = SortDirection.DESC,
                        ),
                    )
                }
            }
        }

        When("키워드와 카테고리 파라미터를 전달하면") {
            Then("해당 파라미터가 Command에 반영된다") {
                val page = PageImpl<ProductSummaryResponse>(emptyList(), PageRequest.of(0, 20), 0L)
                every { searchProductUseCase.execute(any()) } returns page

                mockMvc.get("/api/v1/products") {
                    param("keyword", "맥북")
                    param("category", "ELECTRONICS")
                    param("minPrice", "10000")
                    param("maxPrice", "500000")
                    param("rentalUnit", "DAILY")
                    param("page", "1")
                    param("size", "10")
                    param("sortBy", "NAME")
                    param("sortDirection", "ASC")
                }.andExpect {
                    status { isOk() }
                }

                verify {
                    searchProductUseCase.execute(
                        SearchProductCommand(
                            keyword = "맥북",
                            categoryCode = "ELECTRONICS",
                            status = ProductStatus.AVAILABLE,
                            minPrice = 10000L,
                            maxPrice = 500000L,
                            rentalUnit = RentalUnit.DAILY,
                            page = 1,
                            size = 10,
                            sortBy = ProductSortBy.NAME,
                            sortDirection = SortDirection.ASC,
                        ),
                    )
                }
            }
        }

        When("status 파라미터를 명시적으로 전달하면") {
            Then("해당 status가 Command에 반영된다") {
                val page = PageImpl<ProductSummaryResponse>(emptyList(), PageRequest.of(0, 20), 0L)
                every { searchProductUseCase.execute(any()) } returns page

                mockMvc.get("/api/v1/products") {
                    param("status", "RENTED")
                }.andExpect {
                    status { isOk() }
                }

                verify {
                    searchProductUseCase.execute(
                        match { it.status == ProductStatus.RENTED },
                    )
                }
            }
        }

        When("검색 결과가 비어있으면") {
            Then("빈 페이지가 반환된다") {
                val page = PageImpl<ProductSummaryResponse>(emptyList(), PageRequest.of(0, 20), 0L)
                every { searchProductUseCase.execute(any()) } returns page

                mockMvc.get("/api/v1/products") {
                    param("keyword", "존재하지않는상품")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(0) }
                    jsonPath("$.totalElements") { value(0) }
                }
            }
        }
    }

    Given("GET /api/v1/products/{productId}") {

        When("존재하는 상품 ID로 조회하면") {
            Then("200 OK와 상품 상세 정보가 반환된다") {
                val now = ZonedDateTime.now()
                val detailResponse = ProductDetailResponse(
                    id = 1L,
                    userId = 100L,
                    name = "맥북 프로",
                    description = "최신형 맥북 프로입니다",
                    categoryCode = "ELECTRONICS",
                    condition = ProductCondition.LIKE_NEW,
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 500000L,
                    prices = listOf(
                        ProductDetailResponse.PriceResponse(
                            id = 10L,
                            rentalUnit = RentalUnit.DAILY,
                            priceAmount = 30000L,
                        ),
                    ),
                    images = listOf(
                        ProductDetailResponse.ImageResponse(
                            id = 20L,
                            objectKey = "products/uuid-001.jpg",
                            originalFilename = "front.jpg",
                            sortOrder = 1,
                        ),
                    ),
                    createdAt = now,
                    updatedAt = now,
                )

                every {
                    getProductDetailUseCase.execute(productId = 1L, requestUserId = null)
                } returns detailResponse

                val result = mockMvc.get("/api/v1/products/1")

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(1) }
                    jsonPath("$.name") { value("맥북 프로") }
                    jsonPath("$.description") { value("최신형 맥북 프로입니다") }
                    jsonPath("$.categoryCode") { value("ELECTRONICS") }
                    jsonPath("$.condition") { value("LIKE_NEW") }
                    jsonPath("$.status") { value("AVAILABLE") }
                    jsonPath("$.depositAmount") { value(500000) }
                    jsonPath("$.prices.length()") { value(1) }
                    jsonPath("$.prices[0].rentalUnit") { value("DAILY") }
                    jsonPath("$.prices[0].priceAmount") { value(30000) }
                    jsonPath("$.images.length()") { value(1) }
                    jsonPath("$.images[0].objectKey") { value("products/uuid-001.jpg") }
                }
            }
        }

        When("로그인 상태에서 조회하면") {
            Then("requestUserId가 전달된다") {
                setAuthentication(userId = 42L)

                val now = ZonedDateTime.now()
                val detailResponse = ProductDetailResponse(
                    id = 5L,
                    userId = 42L,
                    name = "테스트 상품",
                    description = "테스트 설명",
                    categoryCode = "ELECTRONICS",
                    condition = ProductCondition.GOOD,
                    status = ProductStatus.DRAFT,
                    depositAmount = 100000L,
                    prices = emptyList(),
                    images = emptyList(),
                    createdAt = now,
                    updatedAt = now,
                )

                every {
                    getProductDetailUseCase.execute(productId = 5L, requestUserId = 42L)
                } returns detailResponse

                val result = mockMvc.get("/api/v1/products/5")

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(5) }
                }

                verify {
                    getProductDetailUseCase.execute(productId = 5L, requestUserId = 42L)
                }
            }
        }

        When("존재하지 않는 상품 ID로 조회하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                every {
                    getProductDetailUseCase.execute(productId = 999L, requestUserId = null)
                } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                    message = "상품을 찾을 수 없습니다 (id=999)",
                )

                val result = mockMvc.get("/api/v1/products/999")

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }

        When("접근 권한이 없는 상품을 조회하면") {
            Then("403 PRODUCT_OWNERSHIP_DENIED 에러가 반환된다") {
                every {
                    getProductDetailUseCase.execute(productId = 10L, requestUserId = null)
                } throws BusinessException(
                    errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                    message = "해당 상품에 접근할 권한이 없습니다 (productId=10)",
                )

                val result = mockMvc.get("/api/v1/products/10")

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("PRODUCT_OWNERSHIP_DENIED") }
                }
            }
        }
    }
})
