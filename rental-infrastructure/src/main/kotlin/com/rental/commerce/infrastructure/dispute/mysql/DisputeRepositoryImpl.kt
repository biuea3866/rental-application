package com.rental.commerce.infrastructure.dispute.mysql

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeRepository
import com.rental.commerce.domain.dispute.DisputeStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import kotlin.math.ceil

/**
 * DisputeRepositoryImpl — JPA 기반 분쟁 Repository 구현체 (ADR-009).
 *
 * 활성 분쟁(OPEN/UNDER_REVIEW) 조회는 상태 IN 절 + DB generated column UNIQUE 가 이중 방어.
 */
@Repository
class DisputeRepositoryImpl(
    private val disputeJpaRepository: DisputeJpaRepository,
) : DisputeRepository {

    override fun save(dispute: Dispute): Dispute = disputeJpaRepository.save(dispute)

    override fun findById(id: Long): Dispute? = disputeJpaRepository.findById(id).orElse(null)

    override fun findActiveByRentalId(rentalId: Long): Dispute? =
        disputeJpaRepository.findByRentalIdAndStatusIn(rentalId, ACTIVE_STATUSES)

    override fun existsActiveByRentalId(rentalId: Long): Boolean =
        disputeJpaRepository.existsByRentalIdAndStatusIn(rentalId, ACTIVE_STATUSES)

    override fun findByOpenerId(openerId: Long, pageQuery: PageQuery): PageResult<Dispute> {
        val page = disputeJpaRepository.findAllByOpenerIdOrderByCreatedAtDesc(
            openerId,
            PageRequest.of(pageQuery.page, pageQuery.size),
        )
        return PageResult(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = if (page.totalElements == 0L) 0
            else ceil(page.totalElements.toDouble() / pageQuery.size).toInt(),
        )
    }

    override fun findByStatus(status: DisputeStatus, pageQuery: PageQuery): PageResult<Dispute> {
        val page = disputeJpaRepository.findAllByStatusOrderByCreatedAtDesc(
            status,
            PageRequest.of(pageQuery.page, pageQuery.size),
        )
        return PageResult(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = if (page.totalElements == 0L) 0
            else ceil(page.totalElements.toDouble() / pageQuery.size).toInt(),
        )
    }

    companion object {
        private val ACTIVE_STATUSES = listOf(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW)
    }
}
