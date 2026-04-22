package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.DisputeDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * GetDisputeUseCase — 분쟁 상세 조회.
 *
 * 접근 권한 검증은 Dispute Entity.verifyAccessibleBy() 로 캡슐화 (Rich Domain Model).
 * 관리자 판정(isAdmin)은 Controller 레이어에서 X-Member-Role 헤더로 주입.
 */
@Service
@Transactional(readOnly = true)
class GetDisputeUseCase(
    private val disputeDomainService: DisputeDomainService,
) {
    fun execute(disputeId: Long, requesterId: Long, isAdmin: Boolean): DisputeResult {
        val dispute = disputeDomainService.getById(disputeId)
        dispute.verifyAccessibleBy(requesterId, isAdmin)
        return DisputeResult.from(dispute)
    }
}
