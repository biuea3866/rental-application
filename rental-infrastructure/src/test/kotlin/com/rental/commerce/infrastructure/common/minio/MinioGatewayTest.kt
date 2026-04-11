package com.rental.commerce.infrastructure.common.minio

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.minio.MinioClient
import io.minio.MakeBucketArgs
import io.minio.PutObjectArgs
import org.testcontainers.containers.MinIOContainer
import java.io.ByteArrayInputStream

class MinioGatewayTest : BehaviorSpec({

    val minioContainer = MinIOContainer("minio/minio:RELEASE.2024-01-18T22-51-28Z")

    beforeSpec {
        minioContainer.start()
    }

    afterSpec {
        minioContainer.stop()
    }

    fun createMinioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(minioContainer.s3URL)
            .credentials(minioContainer.userName, minioContainer.password)
            .build()
    }

    fun createGateway(minioClient: MinioClient): MinioGatewayImpl {
        val buckets = mapOf(
            "products" to "products",
            "inspections" to "inspections",
            "documents" to "documents",
            "chat" to "chat",
        )
        val properties = MinioProperties(
            endpoint = minioContainer.s3URL,
            accessKey = minioContainer.userName,
            secretKey = minioContainer.password,
            buckets = buckets,
        )
        return MinioGatewayImpl(minioClient, properties)
    }

    fun initBuckets(minioClient: MinioClient) {
        listOf("products", "inspections", "documents", "chat").forEach { bucketName ->
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            )
        }
    }

    Given("MinioGatewayImpl - Presigned URL 생성") {

        val minioClient = createMinioClient()
        initBuckets(minioClient)
        val minioGateway = createGateway(minioClient)

        When("products 버킷에 jpg 파일의 Presigned URL을 생성하면") {
            val result = minioGateway.generatePresignedUrl(
                bucket = "products",
                fileName = "test-image.jpg",
                contentType = "image/jpeg",
                expiryMinutes = 15,
            )

            Then("유효한 업로드 URL이 반환된다") {
                result.uploadUrl.shouldNotBeBlank()
                result.uploadUrl shouldContain "products"
            }

            Then("UUID 기반 objectKey가 반환된다") {
                result.objectKey.shouldNotBeBlank()
                result.objectKey shouldContain ".jpg"
                // UUID 형식 검증 (8-4-4-4-12)
                val uuidPart = result.objectKey.substringBeforeLast(".")
                uuidPart.length shouldBe 36
            }
        }

        When("inspections 버킷에 png 파일의 Presigned URL을 생성하면") {
            val result = minioGateway.generatePresignedUrl(
                bucket = "inspections",
                fileName = "inspection-photo.png",
                contentType = "image/png",
                expiryMinutes = 30,
            )

            Then("inspections 버킷 URL이 반환된다") {
                result.uploadUrl.shouldNotBeBlank()
                result.uploadUrl shouldContain "inspections"
            }

            Then("png 확장자를 가진 objectKey가 반환된다") {
                result.objectKey shouldContain ".png"
            }
        }

        When("documents 버킷에 PDF 파일의 Presigned URL을 생성하면") {
            val result = minioGateway.generatePresignedUrl(
                bucket = "documents",
                fileName = "contract.pdf",
                contentType = "application/pdf",
                expiryMinutes = 10,
            )

            Then("유효한 URL과 pdf 확장자 objectKey가 반환된다") {
                result.uploadUrl.shouldNotBeBlank()
                result.objectKey shouldContain ".pdf"
            }
        }

        When("chat 버킷에 파일의 Presigned URL을 생성하면") {
            val result = minioGateway.generatePresignedUrl(
                bucket = "chat",
                fileName = "message-attachment.webp",
                contentType = "image/webp",
            )

            Then("기본 expiry(15분)로 URL이 생성된다") {
                result.uploadUrl.shouldNotBeBlank()
                result.uploadUrl shouldContain "chat"
            }
        }
    }

    Given("MinioGatewayImpl - 존재하지 않는 버킷") {

        val minioClient = createMinioClient()
        val minioGateway = createGateway(minioClient)

        When("등록되지 않은 버킷명으로 URL 생성을 시도하면") {
            Then("BUCKET_NOT_FOUND 에러의 BusinessException이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    minioGateway.generatePresignedUrl(
                        bucket = "unknown-bucket",
                        fileName = "test.jpg",
                        contentType = "image/jpeg",
                    )
                }
                exception.errorCode shouldBe ErrorCode.BUCKET_NOT_FOUND
            }
        }

        When("등록되지 않은 버킷의 objectExists를 호출하면") {
            Then("BUCKET_NOT_FOUND 에러의 BusinessException이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    minioGateway.objectExists("unknown-bucket", "some-key.jpg")
                }
                exception.errorCode shouldBe ErrorCode.BUCKET_NOT_FOUND
            }
        }

        When("등록되지 않은 버킷의 deleteObject를 호출하면") {
            Then("BUCKET_NOT_FOUND 에러의 BusinessException이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    minioGateway.deleteObject("unknown-bucket", "some-key.jpg")
                }
                exception.errorCode shouldBe ErrorCode.BUCKET_NOT_FOUND
            }
        }
    }

    Given("MinioGatewayImpl - 파일 존재 확인") {

        val minioClient = createMinioClient()
        val minioGateway = createGateway(minioClient)

        When("MinIO에 파일을 직접 업로드한 후 objectExists를 호출하면") {
            val objectKey = "test-exists-check.jpg"
            val content = "fake-image-content".toByteArray()
            val inputStream = ByteArrayInputStream(content)

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket("products")
                    .`object`(objectKey)
                    .stream(inputStream, content.size.toLong(), -1)
                    .contentType("image/jpeg")
                    .build()
            )

            Then("true가 반환된다") {
                val exists = minioGateway.objectExists("products", objectKey)
                exists shouldBe true
            }
        }

        When("존재하지 않는 objectKey로 objectExists를 호출하면") {
            Then("false가 반환된다") {
                val exists = minioGateway.objectExists("products", "non-existent-key.jpg")
                exists shouldBe false
            }
        }
    }

    Given("MinioGatewayImpl - 파일 삭제") {

        val minioClient = createMinioClient()
        val minioGateway = createGateway(minioClient)

        When("업로드된 파일을 deleteObject로 삭제하면") {
            val objectKey = "test-delete-target.jpg"
            val content = "fake-image-for-delete".toByteArray()
            val inputStream = ByteArrayInputStream(content)

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket("products")
                    .`object`(objectKey)
                    .stream(inputStream, content.size.toLong(), -1)
                    .contentType("image/jpeg")
                    .build()
            )

            // 삭제 전 존재 확인
            val existsBefore = minioGateway.objectExists("products", objectKey)

            minioGateway.deleteObject("products", objectKey)

            // 삭제 후 존재 확인
            val existsAfter = minioGateway.objectExists("products", objectKey)

            Then("삭제 전에는 파일이 존재한다") {
                existsBefore shouldBe true
            }

            Then("삭제 후에는 파일이 존재하지 않는다") {
                existsAfter shouldBe false
            }
        }

        When("존재하지 않는 objectKey를 삭제해도") {
            Then("예외가 발생하지 않는다") {
                // MinIO의 removeObject는 존재하지 않는 객체에 대해 예외를 던지지 않음
                minioGateway.deleteObject("products", "non-existent-delete-target.jpg")
            }
        }
    }
})
