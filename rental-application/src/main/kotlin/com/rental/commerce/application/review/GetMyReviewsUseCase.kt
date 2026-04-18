package com.rental.commerce.application.review

import com.rental.commerce.domain.review.ReviewDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * GetMyReviewsUseCase
 *
 * 내 리뷰 목록 조회 UseCase — DomainService만 호출하여 오케스트레이션.
 */
@Service
@Transactional(readOnly = true)
class GetMyReviewsUseCase(
    private val reviewDomainService: ReviewDomainService,
) {

    fun execute(command: GetMyReviewsCommand): ReviewListResult {
        val pageResult = reviewDomainService.getMyReviews(command.renterId, command.pageQuery)
        return ReviewListResult.from(pageResult)
    }
}
