package com.rental.commerce.application.review

import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.review.ReviewDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CreateReviewUseCase
 *
 * 리뷰 작성 UseCase — DomainService만 호출하여 오케스트레이션.
 * 대여 상태(RETURNED) 검증 및 리뷰 생성은 각 DomainService에 위임한다.
 */
@Service
@Transactional
class CreateReviewUseCase(
    private val rentalDomainService: RentalDomainService,
    private val reviewDomainService: ReviewDomainService,
) {

    fun execute(command: CreateReviewCommand): ReviewResult = with(command) {
        val rental = rentalDomainService.getRentalById(rentalId)
        rental.verifyRenterAuthority(renterId)
        rental.validateReturned()
        val review = reviewDomainService.createReview(renterId, rentalId, productId, rating, content)
        ReviewResult.from(review)
    }
}
