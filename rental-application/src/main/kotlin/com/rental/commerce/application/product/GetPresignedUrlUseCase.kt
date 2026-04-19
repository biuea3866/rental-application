package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.stereotype.Service

@Service
class GetPresignedUrlUseCase(
    private val productDomainService: ProductDomainService,
) {

    fun execute(command: GetPresignedUrlCommand): PresignedUrlResponse {
        val result = productDomainService.generatePresignedUrl(
            bucket = command.bucket,
            fileName = command.fileName,
            contentType = command.contentType,
        )

        return PresignedUrlResponse(
            uploadUrl = result.uploadUrl,
            objectKey = result.objectKey,
        )
    }
}
