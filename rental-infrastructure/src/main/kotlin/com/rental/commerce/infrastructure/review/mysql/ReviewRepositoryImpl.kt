package com.rental.commerce.infrastructure.review.mysql

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.review.QReview.review
import com.rental.commerce.domain.review.RatingSnapshot
import com.rental.commerce.domain.review.Review
import com.rental.commerce.domain.review.ReviewRepository
import java.math.BigDecimal
import java.math.RoundingMode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class ReviewRepositoryImpl(
    private val reviewJpaRepository: ReviewJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ReviewRepository {

    override fun save(review: Review): Review {
        return reviewJpaRepository.save(review)
    }

    override fun findByRentalId(rentalId: Long): Review? {
        return reviewJpaRepository.findByRentalId(rentalId)
    }

    override fun findByProductId(productId: Long, pageQuery: PageQuery): PageResult<Review> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = reviewJpaRepository.findAllByProductId(productId, pageable)
        return PageResult(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    override fun findByRenterId(renterId: Long, pageQuery: PageQuery): PageResult<Review> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = reviewJpaRepository.findAllByRenterId(renterId, pageable)
        return PageResult(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    override fun existsByRentalId(rentalId: Long): Boolean {
        return reviewJpaRepository.existsByRentalId(rentalId)
    }

    override fun calculateRatingSnapshot(productId: Long): RatingSnapshot {
        val result = queryFactory
            .select(review.rating.avg(), review.count())
            .from(review)
            .where(review.productId.eq(productId))
            .fetchOne()

        val avgDouble = result?.get(review.rating.avg()) ?: 0.0
        val count = result?.get(review.count())?.toInt() ?: 0

        return RatingSnapshot(
            average = BigDecimal.valueOf(avgDouble).setScale(2, RoundingMode.HALF_UP),
            count = count,
        )
    }
}
