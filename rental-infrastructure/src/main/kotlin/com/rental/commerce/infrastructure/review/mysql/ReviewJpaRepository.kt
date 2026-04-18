package com.rental.commerce.infrastructure.review.mysql

import com.rental.commerce.domain.review.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewJpaRepository : JpaRepository<Review, Long> {

    fun findByRentalId(rentalId: Long): Review?

    fun findAllByProductId(productId: Long, pageable: Pageable): Page<Review>

    fun findAllByRenterId(renterId: Long, pageable: Pageable): Page<Review>

    fun existsByRentalId(rentalId: Long): Boolean
}
