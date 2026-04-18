package com.rental.commerce.presentation.api.review

import com.rental.commerce.application.review.CreateReviewCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateReviewRequest(

    @field:NotNull(message = "productId는 필수입니다.")
    val productId: Long?,

    @field:NotNull(message = "rentalId는 필수입니다.")
    val rentalId: Long?,

    @field:NotNull(message = "rating은 필수입니다.")
    val rating: Int?,

    @field:NotBlank(message = "content는 필수입니다.")
    val content: String?,
) {
    fun toCommand(userId: Long): CreateReviewCommand = CreateReviewCommand(
        renterId = userId,
        rentalId = requireNotNull(rentalId),
        productId = requireNotNull(productId),
        rating = requireNotNull(rating),
        content = requireNotNull(content),
    )
}
