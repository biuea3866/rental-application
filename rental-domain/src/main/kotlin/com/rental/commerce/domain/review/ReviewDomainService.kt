package com.rental.commerce.domain.review

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.ReviewAlreadyExistsException
import com.rental.commerce.domain.review.event.ReviewCreatedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * ReviewDomainService
 *
 * Entity + Port(ReviewRepository) 조합을 수행하는 도메인 서비스.
 * 중복 리뷰 검증은 이 서비스에서 담당한다.
 *
 * @Transactional 은 UseCase 레이어에서 선언한다 — harness transaction.default 참고.
 */
@Service
class ReviewDomainService(
    private val reviewRepository: ReviewRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /**
     * 신규 리뷰를 생성한다.
     * - 동일 rentalId에 대한 중복 리뷰 방지
     * - rating 범위 검증은 Review.create() init 블록에서 수행
     */
    fun createReview(
        renterId: Long,
        rentalId: Long,
        productId: Long,
        rating: Int,
        content: String,
    ): Review {
        if (reviewRepository.existsByRentalId(rentalId)) {
            throw ReviewAlreadyExistsException("이미 리뷰가 작성된 대여입니다. rentalId=$rentalId")
        }

        val review = Review.create(
            renterId = renterId,
            rentalId = rentalId,
            productId = productId,
            rating = rating,
            content = content,
        )

        val saved = reviewRepository.save(review)
        eventPublisher.publishEvent(
            ReviewCreatedEvent(
                reviewId = saved.id,
                productId = saved.productId,
                renterId = saved.renterId,
                rating = saved.rating,
            ),
        )
        return saved
    }

    /**
     * 특정 상품의 리뷰 목록을 조회한다 (페이지네이션 포함).
     */
    fun getProductReviews(productId: Long, pageQuery: PageQuery): PageResult<Review> {
        return reviewRepository.findByProductId(productId, pageQuery)
    }

    /**
     * 특정 대여자의 리뷰 목록을 조회한다 (페이지네이션 포함).
     */
    fun getMyReviews(renterId: Long, pageQuery: PageQuery): PageResult<Review> {
        return reviewRepository.findByRenterId(renterId, pageQuery)
    }
}
