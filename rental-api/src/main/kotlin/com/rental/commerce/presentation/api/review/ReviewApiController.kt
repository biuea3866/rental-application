package com.rental.commerce.presentation.api.review

import com.rental.commerce.application.review.CreateReviewUseCase
import com.rental.commerce.application.review.GetMyReviewsCommand
import com.rental.commerce.application.review.GetMyReviewsUseCase
import com.rental.commerce.application.review.GetProductReviewsCommand
import com.rental.commerce.application.review.GetProductReviewsUseCase
import com.rental.commerce.application.review.ReviewListResult
import com.rental.commerce.application.review.ReviewResult
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ReviewApiController(
    private val createReviewUseCase: CreateReviewUseCase,
    private val getProductReviewsUseCase: GetProductReviewsUseCase,
    private val getMyReviewsUseCase: GetMyReviewsUseCase,
) {

    @PostMapping("/reviews")
    fun createReview(
        @AuthenticatedMember userId: Long,
        @Valid @RequestBody request: CreateReviewRequest,
    ): ResponseEntity<ReviewResult> {
        val result = createReviewUseCase.execute(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping("/products/{productId}/reviews")
    fun getProductReviews(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<ReviewListResult> {
        val result = getProductReviewsUseCase.execute(
            GetProductReviewsCommand(
                productId = productId,
                pageQuery = PageQuery(page = page, size = size),
            ),
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/my-reviews")
    fun getMyReviews(
        @AuthenticatedMember userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<ReviewListResult> {
        val result = getMyReviewsUseCase.execute(
            GetMyReviewsCommand(
                renterId = userId,
                pageQuery = PageQuery(page = page, size = size),
            ),
        )
        return ResponseEntity.ok(result)
    }
}
