package com.rental.commerce.application.dispute

import com.rental.commerce.domain.dispute.DisputeReason

data class OpenDisputeCommand(
    val openerId: Long,
    val rentalId: Long,
    val reason: DisputeReason,
    val description: String,
    val attachmentUrls: List<String> = emptyList(),
)
