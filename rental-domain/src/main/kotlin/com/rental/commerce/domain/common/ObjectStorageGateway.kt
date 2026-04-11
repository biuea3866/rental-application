package com.rental.commerce.domain.common

data class PresignedUrlResult(
    val uploadUrl: String,
    val objectKey: String,
)

interface ObjectStorageGateway {
    fun generatePresignedUrl(
        bucket: String,
        fileName: String,
        contentType: String,
        expiryMinutes: Int = 15,
    ): PresignedUrlResult

    fun deleteObject(bucket: String, objectKey: String)

    fun objectExists(bucket: String, objectKey: String): Boolean
}
