package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.DisputeDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CancelDisputeUseCase — 분쟁 오픈자가 OPEN 상태에서만 스스로 취소.
 * (UNDER_REVIEW 진입 후에는 취소 불가 — DisputeStatus.validateCanCancel)
 */
@Service
@Transactional
class CancelDisputeUseCase(
    private val disputeDomainService: DisputeDomainService,
) {
    fun execute(disputeId: Long, requesterId: Long): DisputeResult {
        val dispute = disputeDomainService.cancelByOpener(disputeId, requesterId)
        return DisputeResult.from(dispute)
    }
}
