package com.rental.commerce.application.review

import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.review.Review
import java.time.ZonedDateTime

data class ReviewResult(
    val reviewId: Long,
    val renterId: Long,
    val rentalId: Long,
    val productId: Long,
    val rating: Int,
    val content: String,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(review: Review): ReviewResult = ReviewResult(
            reviewId = review.id,
            renterId = review.renterId,
            rentalId = review.rentalId,
            productId = review.productId,
            rating = review.rating,
            content = review.content,
            createdAt = review.createdAt,
        )
    }
}

data class ReviewListResult(
    val reviews: List<ReviewResult>,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(pageResult: PageResult<Review>): ReviewListResult = ReviewListResult(
            reviews = pageResult.content.map { ReviewResult.from(it) },
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
        )
    }
}
