package com.rental.commerce.infrastructure.dispute.mysql

import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface DisputeJpaRepository : JpaRepository<Dispute, Long> {

    fun findAllByOpenerIdOrderByCreatedAtDesc(openerId: Long, pageable: Pageable): Page<Dispute>

    fun findAllByStatusOrderByCreatedAtDesc(status: DisputeStatus, pageable: Pageable): Page<Dispute>

    /**
     * 활성(OPEN/UNDER_REVIEW) 분쟁을 rental 기준으로 조회한다.
     * DB active_rental_id 가 NULL 이 아닌 행(=활성)만 대상이 됨.
     */
    fun findByRentalIdAndStatusIn(rentalId: Long, statuses: Collection<DisputeStatus>): Dispute?

    fun existsByRentalIdAndStatusIn(rentalId: Long, statuses: Collection<DisputeStatus>): Boolean
}
