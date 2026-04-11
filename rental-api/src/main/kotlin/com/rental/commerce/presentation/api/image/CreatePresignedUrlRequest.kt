package com.rental.commerce.presentation.api.image

import com.rental.commerce.application.product.GetPresignedUrlCommand
import jakarta.validation.constraints.NotBlank

data class CreatePresignedUrlRequest(
    @field:NotBlank(message = "파일명은 필수입니다")
    val fileName: String,

    @field:NotBlank(message = "Content-Type은 필수입니다")
    val contentType: String,

    @field:NotBlank(message = "버킷은 필수입니다")
    val bucket: String,
) {
    fun toCommand(): GetPresignedUrlCommand = GetPresignedUrlCommand(
        bucket = bucket,
        fileName = fileName,
        contentType = contentType,
    )
}
