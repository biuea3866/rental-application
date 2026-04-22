package com.rental.commerce.presentation.api.dispute

import com.rental.commerce.application.dispute.ResolveDisputeCommand
import java.math.BigDecimal

data class ResolveDisputeRequest(
    val type: ResolveDisputeCommand.ResolutionType,
    val refundAmount: BigDecimal? = null,
) {
    fun toCommand(disputeId: Long) = ResolveDisputeCommand(
        disputeId = disputeId,
        type = type,
        refundAmount = refundAmount,
    )
}
