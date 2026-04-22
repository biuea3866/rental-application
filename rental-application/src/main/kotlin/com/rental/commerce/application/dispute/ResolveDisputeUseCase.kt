package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.DisputeDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ResolveDisputeUseCase — 관리자가 UNDER_REVIEW → RESOLVED_* 종결.
 *
 * - DomainService 경유, 이벤트(DisputeResolvedEvent) 발행은 DomainService 에 위임.
 * - @Transactional 로 감싸 DB 상태 전이와 이벤트 발행이 동일 트랜잭션 경계.
 *   AFTER_COMMIT 리스너(BE-405 RefundProcessingListener)에서 환불 처리.
 */
@Service
@Transactional
class ResolveDisputeUseCase(
    private val disputeDomainService: DisputeDomainService,
) {
    fun execute(command: ResolveDisputeCommand): DisputeResult {
        val dispute = when (command.type) {
            ResolveDisputeCommand.ResolutionType.FULL_REFUND ->
                disputeDomainService.resolveFullRefund(command.disputeId, command.refundAmount!!)
            ResolveDisputeCommand.ResolutionType.PARTIAL ->
                disputeDomainService.resolvePartial(command.disputeId, command.refundAmount!!)
            ResolveDisputeCommand.ResolutionType.REJECTED ->
                disputeDomainService.resolveRejected(command.disputeId)
        }
        return DisputeResult.from(dispute)
    }
}
