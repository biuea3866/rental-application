package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.RejectProductCommand
import jakarta.validation.constraints.NotBlank

data class RejectProductRequest(
    @field:NotBlank(message = "반려 사유는 필수입니다")
    val reason: String,
) {
    fun toCommand(productId: Long): RejectProductCommand =
        RejectProductCommand(
            productId = productId,
            reason = reason,
        )
}
