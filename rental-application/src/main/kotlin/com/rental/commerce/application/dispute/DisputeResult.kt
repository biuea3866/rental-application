package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.Dispute
import com.rental.commerce.domain.dispute.DisputeReason
import com.rental.commerce.domain.dispute.DisputeStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class DisputeResult(
    val id: Long,
    val rentalId: Long,
    val openerId: Long,
    val reason: DisputeReason,
    val description: String,
    val status: DisputeStatus,
    val refundAmount: BigDecimal?,
    val resolvedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(dispute: Dispute) = DisputeResult(
            id = dispute.id,
            rentalId = dispute.rentalId,
            openerId = dispute.openerId,
            reason = dispute.reason,
            description = dispute.description,
            status = dispute.status,
            refundAmount = dispute.refundAmount,
            resolvedAt = dispute.resolvedAt,
            createdAt = dispute.createdAt,
        )
    }
}
