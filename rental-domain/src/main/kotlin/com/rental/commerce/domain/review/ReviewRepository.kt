package com.rental.commerce.domain.review

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult

interface ReviewRepository {

    fun save(review: Review): Review

    fun findByRentalId(rentalId: Long): Review?

    fun findByProductId(productId: Long, pageQuery: PageQuery): PageResult<Review>

    fun findByRenterId(renterId: Long, pageQuery: PageQuery): PageResult<Review>

    fun existsByRentalId(rentalId: Long): Boolean
}
