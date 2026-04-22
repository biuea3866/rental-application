package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.DisputeDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * StartDisputeReviewUseCase — 관리자가 OPEN → UNDER_REVIEW 전환.
 */
@Service
@Transactional
class StartDisputeReviewUseCase(
    private val disputeDomainService: DisputeDomainService,
) {
    fun execute(disputeId: Long): DisputeResult {
        val dispute = disputeDomainService.startReview(disputeId)
        return DisputeResult.from(dispute)
    }
}
