package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ObjectStorageGateway
import org.springframework.stereotype.Service

@Service
class GetPresignedUrlUseCase(
    private val objectStorageGateway: ObjectStorageGateway,
) {

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
        )
    }

    fun execute(command: GetPresignedUrlCommand): PresignedUrlResponse {
        validateContentType(command.contentType)

        val result = objectStorageGateway.generatePresignedUrl(
            bucket = command.bucket,
            fileName = command.fileName,
            contentType = command.contentType,
        )

        return PresignedUrlResponse(
            uploadUrl = result.uploadUrl,
            objectKey = result.objectKey,
        )
    }

    private fun validateContentType(contentType: String) {
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException(
                errorCode = ErrorCode.INVALID_FILE_TYPE,
                message = "지원하지 않는 파일 형식입니다: $contentType",
            )
        }
    }
}
