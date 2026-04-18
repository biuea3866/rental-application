package com.rental.commerce.application.review

import com.rental.commerce.domain.review.ReviewDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * GetProductReviewsUseCase
 *
 * 특정 상품의 리뷰 목록 조회 UseCase — DomainService만 호출하여 오케스트레이션.
 */
@Service
@Transactional(readOnly = true)
class GetProductReviewsUseCase(
    private val reviewDomainService: ReviewDomainService,
) {

    fun execute(command: GetProductReviewsCommand): ReviewListResult {
        val pageResult = reviewDomainService.getProductReviews(command.productId, command.pageQuery)
        return ReviewListResult.from(pageResult)
    }
}
