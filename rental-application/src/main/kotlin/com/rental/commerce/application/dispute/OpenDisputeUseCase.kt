package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.DisputeDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OpenDisputeUseCase — 대여자/등록자가 대여 건에 대해 분쟁 오픈.
 *
 * - UseCase 는 DomainService 만 호출 (CLAUDE.md: UseCase→Repository 직접 호출 금지)
 * - 첨부 이미지 저장은 DomainService 에 위임 (BE-402 이후 첨부 처리는 별도 티켓에서)
 */
@Service
@Transactional
class OpenDisputeUseCase(
    private val disputeDomainService: DisputeDomainService,
) {
    fun execute(command: OpenDisputeCommand): DisputeResult {
        val dispute = disputeDomainService.openDispute(
            rentalId = command.rentalId,
            openerId = command.openerId,
            reason = command.reason,
            description = command.description,
        )
        return DisputeResult.from(dispute)
    }
}
