package com.rental.commerce.domain.dispute

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.dispute.event.DisputeCreatedEvent
import com.rental.commerce.domain.dispute.event.DisputeResolvedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * DisputeDomainService (ADR-009).
 *
 * - 활성 분쟁 단일 보장 (사전 조회 + DB UNIQUE 보조)
 * - 상태 전이는 Dispute Entity 내부에 캡슐화
 * - 이벤트 발행은 도메인 서비스가 책임
 * - @Transactional 은 UseCase 에서 선언 (CLAUDE.md)
 */
@Service
class DisputeDomainService(
    private val disputeRepository: DisputeRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun openDispute(
        rentalId: Long,
        openerId: Long,
        reason: DisputeReason,
        description: String,
    ): Dispute {
        if (disputeRepository.existsActiveByRentalId(rentalId)) {
            throw DisputeAlreadyActiveException(
                "이미 진행 중인 분쟁이 있습니다. rentalId=$rentalId",
            )
        }
        val dispute = Dispute.create(
            rentalId = rentalId,
            openerId = openerId,
            reason = reason,
            description = description,
        )
        val saved = disputeRepository.save(dispute)
        eventPublisher.publishEvent(
            DisputeCreatedEvent(
                disputeId = saved.id,
                rentalId = saved.rentalId,
                openerId = saved.openerId,
                reason = saved.reason,
            ),
        )
        return saved
    }

    fun startReview(disputeId: Long): Dispute {
        val dispute = getById(disputeId)
        dispute.startReview()
        return disputeRepository.save(dispute)
    }

    fun resolveFullRefund(disputeId: Long, amount: BigDecimal): Dispute {
        val dispute = getById(disputeId)
        dispute.resolveFullRefund(amount)
        val saved = disputeRepository.save(dispute)
        publishResolved(saved)
        return saved
    }

    fun resolvePartial(disputeId: Long, amount: BigDecimal): Dispute {
        val dispute = getById(disputeId)
        dispute.resolvePartial(amount)
        val saved = disputeRepository.save(dispute)
        publishResolved(saved)
        return saved
    }

    fun resolveRejected(disputeId: Long): Dispute {
        val dispute = getById(disputeId)
        dispute.resolveRejected()
        val saved = disputeRepository.save(dispute)
        publishResolved(saved)
        return saved
    }

    fun cancelByOpener(disputeId: Long, byUserId: Long): Dispute {
        val dispute = getById(disputeId)
        dispute.cancel(byUserId)
        return disputeRepository.save(dispute)
    }

    fun getById(id: Long): Dispute =
        disputeRepository.findById(id)
            ?: throw DisputeNotFoundException("분쟁을 찾을 수 없습니다. id=$id")

    fun getByOpener(openerId: Long, pageQuery: PageQuery): PageResult<Dispute> =
        disputeRepository.findByOpenerId(openerId, pageQuery)

    private fun publishResolved(dispute: Dispute) {
        eventPublisher.publishEvent(
            DisputeResolvedEvent(
                disputeId = dispute.id,
                rentalId = dispute.rentalId,
                resolution = dispute.status,
                refundAmount = dispute.refundAmount,
            ),
        )
    }
}
