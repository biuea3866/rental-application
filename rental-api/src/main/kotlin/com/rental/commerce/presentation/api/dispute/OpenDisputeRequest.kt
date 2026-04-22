package com.rental.commerce.presentation.api.dispute

import com.rental.commerce.application.dispute.OpenDisputeCommand
import com.rental.commerce.domain.dispute.DisputeReason
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class OpenDisputeRequest(
    val rentalId: Long,
    val reason: DisputeReason,
    @field:NotBlank
    @field:Size(min = 1, max = 1000, message = "설명은 1~1000자")
    val description: String,
    val attachmentUrls: List<String> = emptyList(),
) {
    fun toCommand(openerId: Long) = OpenDisputeCommand(
        openerId = openerId,
        rentalId = rentalId,
        reason = reason,
        description = description,
        attachmentUrls = attachmentUrls,
    )
}
