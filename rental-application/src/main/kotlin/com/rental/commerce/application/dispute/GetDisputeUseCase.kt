package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.DisputeDomainService
import com.rental.commerce.domain.dispute.DisputeForbiddenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * GetDisputeUseCase — 분쟁 상세 조회.
 *
 * 접근 권한: opener 또는 관리자(isAdmin) 만 허용.
 * 관리자 판정은 Controller 레이어에서 X-Member-Role 헤더로 가려서 isAdmin 파라미터로 주입.
 */
@Service
@Transactional(readOnly = true)
class GetDisputeUseCase(
    private val disputeDomainService: DisputeDomainService,
) {
    fun execute(disputeId: Long, requesterId: Long, isAdmin: Boolean): DisputeResult {
        val dispute = disputeDomainService.getById(disputeId)
        if (!isAdmin && dispute.openerId != requesterId) {
            throw DisputeForbiddenException()
        }
        return DisputeResult.from(dispute)
    }
}
