package com.rental.commerce.presentation.api.review

import com.rental.commerce.application.review.CreateReviewUseCase
import com.rental.commerce.application.review.GetMyReviewsUseCase
import com.rental.commerce.application.review.GetProductReviewsUseCase
import com.rental.commerce.application.review.ReviewListResult
import com.rental.commerce.application.review.ReviewResult
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ReviewApiControllerTest : BehaviorSpec({

    val createReviewUseCase = mockk<CreateReviewUseCase>()
    val getProductReviewsUseCase = mockk<GetProductReviewsUseCase>()
    val getMyReviewsUseCase = mockk<GetMyReviewsUseCase>()

    val controller = ReviewApiController(
        createReviewUseCase = createReviewUseCase,
        getProductReviewsUseCase = getProductReviewsUseCase,
        getMyReviewsUseCase = getMyReviewsUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(createReviewUseCase, getProductReviewsUseCase, getMyReviewsUseCase)
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/reviews — 리뷰 작성
    // ──────────────────────────────────────────────────────────
    Given("POST /api/v1/reviews") {

        When("정상적인 리뷰 작성 요청을 보내면") {
            Then("201 Created와 리뷰 정보가 반환된다") {
                val now = ZonedDateTime.now()
                val response = ReviewResult(
                    reviewId = 1L,
                    renterId = 1L,
                    rentalId = 100L,
                    productId = 42L,
                    rating = 5,
                    content = "정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다.",
                    createdAt = now,
                )

                every { createReviewUseCase.execute(any()) } returns response

                val result = mockMvc.post("/api/v1/reviews") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 42,
                          "rentalId": 100,
                          "rating": 5,
                          "content": "정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다."
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.reviewId") { value(1) }
                    jsonPath("$.renterId") { value(1) }
                    jsonPath("$.rentalId") { value(100) }
                    jsonPath("$.productId") { value(42) }
                    jsonPath("$.rating") { value(5) }
                    jsonPath("$.content") { value("정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다.") }
                }

                verify { createReviewUseCase.execute(any()) }
            }
        }

        When("productId가 누락된 요청을 보내면") {
            Then("400 Bad Request가 반환된다") {
                val result = mockMvc.post("/api/v1/reviews") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "rentalId": 100,
                          "rating": 5,
                          "content": "정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다."
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("반납이 완료되지 않은 대여에 리뷰를 작성하면") {
            Then("422 REVIEW_RENTAL_NOT_RETURNED 에러가 반환된다") {
                every { createReviewUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.REVIEW_RENTAL_NOT_RETURNED,
                )

                val result = mockMvc.post("/api/v1/reviews") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 42,
                          "rentalId": 100,
                          "rating": 5,
                          "content": "정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다."
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isUnprocessableEntity() }
                    jsonPath("$.code") { value("REVIEW_RENTAL_NOT_RETURNED") }
                }
            }
        }

        When("권한 없는 사용자가 리뷰를 작성하면") {
            Then("403 REVIEW_FORBIDDEN 에러가 반환된다") {
                every { createReviewUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.REVIEW_FORBIDDEN,
                )

                val result = mockMvc.post("/api/v1/reviews") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 42,
                          "rentalId": 100,
                          "rating": 5,
                          "content": "정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다."
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("REVIEW_FORBIDDEN") }
                }
            }
        }

        When("이미 리뷰가 작성된 대여에 리뷰를 다시 작성하면") {
            Then("409 REVIEW_ALREADY_EXISTS 에러가 반환된다") {
                every { createReviewUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.REVIEW_ALREADY_EXISTS,
                )

                val result = mockMvc.post("/api/v1/reviews") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 42,
                          "rentalId": 100,
                          "rating": 5,
                          "content": "정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다."
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isConflict() }
                    jsonPath("$.code") { value("REVIEW_ALREADY_EXISTS") }
                }
            }
        }

        When("대여를 찾을 수 없는 경우") {
            Then("404 RENTAL_NOT_FOUND 에러가 반환된다") {
                every { createReviewUseCase.execute(any()) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.RENTAL_NOT_FOUND,
                    message = "대여를 찾을 수 없습니다.",
                )

                val result = mockMvc.post("/api/v1/reviews") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 42,
                          "rentalId": 999,
                          "rating": 5,
                          "content": "정말 좋은 상품이었습니다. 다음에 또 이용하고 싶습니다."
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("RENTAL_NOT_FOUND") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/products/{productId}/reviews — 상품별 리뷰 목록
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/products/{productId}/reviews") {

        When("정상적으로 상품 리뷰 목록을 조회하면") {
            Then("200 OK와 리뷰 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val response = ReviewListResult(
                    reviews = listOf(
                        ReviewResult(
                            reviewId = 1L,
                            renterId = 1L,
                            rentalId = 100L,
                            productId = 42L,
                            rating = 5,
                            content = "좋은 상품입니다.",
                            createdAt = now,
                        ),
                    ),
                    totalElements = 1L,
                    totalPages = 1,
                )

                every { getProductReviewsUseCase.execute(any()) } returns response

                val result = mockMvc.get("/api/v1/products/42/reviews") {
                    param("page", "0")
                    param("size", "10")
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.reviews.length()") { value(1) }
                    jsonPath("$.reviews[0].reviewId") { value(1) }
                    jsonPath("$.reviews[0].productId") { value(42) }
                    jsonPath("$.reviews[0].rating") { value(5) }
                    jsonPath("$.reviews[0].content") { value("좋은 상품입니다.") }
                    jsonPath("$.totalElements") { value(1) }
                    jsonPath("$.totalPages") { value(1) }
                }

                verify { getProductReviewsUseCase.execute(any()) }
            }
        }

        When("리뷰가 없는 상품을 조회하면") {
            Then("200 OK와 빈 목록이 반환된다") {
                val response = ReviewListResult(
                    reviews = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )

                every { getProductReviewsUseCase.execute(any()) } returns response

                val result = mockMvc.get("/api/v1/products/42/reviews") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.reviews.length()") { value(0) }
                    jsonPath("$.totalElements") { value(0) }
                }
            }
        }

        When("존재하지 않는 상품의 리뷰를 조회하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                every { getProductReviewsUseCase.execute(any()) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                    message = "상품을 찾을 수 없습니다.",
                )

                val result = mockMvc.get("/api/v1/products/999/reviews") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/my-reviews — 내 리뷰 목록
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/my-reviews") {

        When("정상적으로 내 리뷰 목록을 조회하면") {
            Then("200 OK와 내 리뷰 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val response = ReviewListResult(
                    reviews = listOf(
                        ReviewResult(
                            reviewId = 1L,
                            renterId = 1L,
                            rentalId = 100L,
                            productId = 42L,
                            rating = 4,
                            content = "사용감이 좋았습니다.",
                            createdAt = now,
                        ),
                        ReviewResult(
                            reviewId = 2L,
                            renterId = 1L,
                            rentalId = 200L,
                            productId = 55L,
                            rating = 3,
                            content = "보통이었습니다.",
                            createdAt = now.minusDays(1),
                        ),
                    ),
                    totalElements = 2L,
                    totalPages = 1,
                )

                every { getMyReviewsUseCase.execute(any()) } returns response

                val result = mockMvc.get("/api/v1/my-reviews") {
                    param("page", "0")
                    param("size", "10")
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.reviews.length()") { value(2) }
                    jsonPath("$.reviews[0].reviewId") { value(1) }
                    jsonPath("$.reviews[0].renterId") { value(1) }
                    jsonPath("$.reviews[0].rating") { value(4) }
                    jsonPath("$.reviews[1].reviewId") { value(2) }
                    jsonPath("$.reviews[1].rating") { value(3) }
                    jsonPath("$.totalElements") { value(2) }
                    jsonPath("$.totalPages") { value(1) }
                }

                verify { getMyReviewsUseCase.execute(any()) }
            }
        }

        When("리뷰가 없는 사용자가 목록을 조회하면") {
            Then("200 OK와 빈 목록이 반환된다") {
                val response = ReviewListResult(
                    reviews = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )

                every { getMyReviewsUseCase.execute(any()) } returns response

                val result = mockMvc.get("/api/v1/my-reviews") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.reviews.length()") { value(0) }
                    jsonPath("$.totalElements") { value(0) }
                }
            }
        }
    }
})
