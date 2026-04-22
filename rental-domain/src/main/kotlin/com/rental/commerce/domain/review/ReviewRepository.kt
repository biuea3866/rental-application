package com.rental.commerce.domain.review

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import java.math.BigDecimal

interface ReviewRepository {

    fun save(review: Review): Review

    fun findByRentalId(rentalId: Long): Review?

    fun findByProductId(productId: Long, pageQuery: PageQuery): PageResult<Review>

    fun findByRenterId(renterId: Long, pageQuery: PageQuery): PageResult<Review>

    fun existsByRentalId(rentalId: Long): Boolean

    /**
     * 상품별 평점 스냅샷 (BE-411, ADR-010).
     * count=0 이면 average=0.
     */
    fun calculateRatingSnapshot(productId: Long): RatingSnapshot
}

data class RatingSnapshot(
    val average: BigDecimal,
    val count: Int,
)
