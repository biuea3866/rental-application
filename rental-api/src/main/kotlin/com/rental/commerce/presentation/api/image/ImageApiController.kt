package com.rental.commerce.presentation.api.image

import com.rental.commerce.application.product.GetPresignedUrlUseCase
import com.rental.commerce.application.product.PresignedUrlResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/images")
class ImageApiController(
    private val getPresignedUrlUseCase: GetPresignedUrlUseCase,
) {

    @PostMapping("/presigned-url")
    fun createPresignedUrl(
        @Valid @RequestBody request: CreatePresignedUrlRequest,
    ): ResponseEntity<PresignedUrlResponse> {
        val response = getPresignedUrlUseCase.execute(request.toCommand())
        return ResponseEntity.ok(response)
    }
}
