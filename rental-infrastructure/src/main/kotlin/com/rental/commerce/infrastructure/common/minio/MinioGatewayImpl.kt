package com.rental.commerce.infrastructure.common.minio

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ObjectStorageGateway
import com.rental.commerce.domain.common.PresignedUrlResult
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class MinioGatewayImpl(
    private val minioClient: MinioClient,
    private val minioProperties: MinioProperties,
) : ObjectStorageGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun generatePresignedUrl(
        bucket: String,
        fileName: String,
        contentType: String,
        expiryMinutes: Int,
    ): PresignedUrlResult {
        val resolvedBucket = resolveBucket(bucket)
        val extension = extractExtension(fileName)
        val objectKey = "${UUID.randomUUID()}.$extension"

        logger.info("Generating presigned URL: bucket=$resolvedBucket, objectKey=$objectKey, contentType=$contentType, expiryMinutes=$expiryMinutes")

        val presignedUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(resolvedBucket)
                .`object`(objectKey)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .build()
        )

        return PresignedUrlResult(
            uploadUrl = presignedUrl,
            objectKey = objectKey,
        )
    }

    override fun deleteObject(bucket: String, objectKey: String) {
        val resolvedBucket = resolveBucket(bucket)

        logger.info("Deleting object: bucket=$resolvedBucket, objectKey=$objectKey")

        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(resolvedBucket)
                .`object`(objectKey)
                .build()
        )
    }

    override fun objectExists(bucket: String, objectKey: String): Boolean {
        val resolvedBucket = resolveBucket(bucket)

        return try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .`object`(objectKey)
                    .build()
            )
            true
        } catch (exception: io.minio.errors.ErrorResponseException) {
            if (exception.errorResponse().code() == "NoSuchKey") {
                false
            } else {
                throw exception
            }
        }
    }

    private fun resolveBucket(bucket: String): String {
        return minioProperties.buckets[bucket]
            ?: throw BusinessException(
                errorCode = ErrorCode.BUCKET_NOT_FOUND,
                message = "등록되지 않은 버킷입니다: $bucket",
            )
    }

    private fun extractExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "bin")
    }
}
