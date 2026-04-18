package com.rental.commerce.application.review

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.review.Review
import com.rental.commerce.domain.review.ReviewDomainService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetProductReviewsUseCaseTest : BehaviorSpec({

    val reviewDomainService = mockk<ReviewDomainService>()
    val useCase = GetProductReviewsUseCase(reviewDomainService)

    beforeEach {
        clearMocks(reviewDomainService)
    }

    Given("상품 리뷰 목록을 조회할 때") {

        When("정상적으로 상품 리뷰 목록을 조회하면") {
            Then("reviewDomainService.getProductReviews()가 1회 호출되고 결과를 반환한다") {
                val productId = 42L
                val pageQuery = PageQuery(page = 0, size = 10)

                val review1 = Review.create(
                    renterId = 1L,
                    rentalId = 10L,
                    productId = productId,
                    rating = 5,
                    content = "상태도 좋고 설명과 동일한 상품이었습니다.",
                )
                val review2 = Review.create(
                    renterId = 2L,
                    rentalId = 11L,
                    productId = productId,
                    rating = 4,
                    content = "전반적으로 만족스러운 대여 경험이었습니다.",
                )

                val pageResult = PageResult(
                    content = listOf(review1, review2),
                    totalElements = 2L,
                    totalPages = 1,
                )

                val command = GetProductReviewsCommand(
                    productId = productId,
                    pageQuery = pageQuery,
                )

                every { reviewDomainService.getProductReviews(productId, pageQuery) } returns pageResult

                val result = useCase.execute(command)

                result shouldNotBe null
                result.reviews.size shouldBe 2
                result.totalElements shouldBe 2L
                result.totalPages shouldBe 1
                result.reviews[0].productId shouldBe productId
                result.reviews[0].rating shouldBe 5
                result.reviews[1].rating shouldBe 4

                verify(exactly = 1) { reviewDomainService.getProductReviews(productId, pageQuery) }
            }
        }

        When("해당 상품에 리뷰가 없는 경우") {
            Then("빈 목록을 반환한다") {
                val productId = 999L
                val pageQuery = PageQuery(page = 0, size = 10)

                val pageResult = PageResult<Review>(
                    content = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )

                val command = GetProductReviewsCommand(
                    productId = productId,
                    pageQuery = pageQuery,
                )

                every { reviewDomainService.getProductReviews(productId, pageQuery) } returns pageResult

                val result = useCase.execute(command)

                result.reviews.size shouldBe 0
                result.totalElements shouldBe 0L

                verify(exactly = 1) { reviewDomainService.getProductReviews(productId, pageQuery) }
            }
        }
    }
})
