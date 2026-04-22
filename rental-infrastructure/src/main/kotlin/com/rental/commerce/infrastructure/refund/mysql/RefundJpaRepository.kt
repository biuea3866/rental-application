package com.rental.commerce.infrastructure.refund.mysql

import com.rental.commerce.domain.refund.Refund
import org.springframework.data.jpa.repository.JpaRepository

interface RefundJpaRepository : JpaRepository<Refund, Long> {

    fun findAllByDisputeId(disputeId: Long): List<Refund>
}
