package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PresignedUrlResult
import com.rental.commerce.domain.product.ProductDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetPresignedUrlUseCaseTest : BehaviorSpec({

    val productDomainService = mockk<ProductDomainService>()
    val useCase = GetPresignedUrlUseCase(productDomainService)

    Given("Presigned URL 생성을 요청할 때") {

        When("지원하는 contentType(image/jpeg)으로 요청하면") {
            val command = GetPresignedUrlCommand(
                bucket = "products",
                fileName = "test-image.jpg",
                contentType = "image/jpeg",
            )

            val expectedResult = PresignedUrlResult(
                uploadUrl = "https://minio.example.com/products/uuid-123.jpg?presigned",
                objectKey = "uuid-123.jpg",
            )

            every {
                productDomainService.generatePresignedUrl(
                    bucket = command.bucket,
                    fileName = command.fileName,
                    contentType = command.contentType,
                )
            } returns expectedResult

            val result = useCase.execute(command)

            Then("uploadUrl과 objectKey가 정상 반환된다") {
                result.uploadUrl shouldBe expectedResult.uploadUrl
                result.objectKey shouldBe expectedResult.objectKey
            }

            Then("ProductDomainService가 정확히 한 번 호출된다") {
                verify(exactly = 1) {
                    productDomainService.generatePresignedUrl(
                        bucket = command.bucket,
                        fileName = command.fileName,
                        contentType = command.contentType,
                    )
                }
            }
        }

        When("지원하는 contentType(image/png)으로 요청하면") {
            val command = GetPresignedUrlCommand(
                bucket = "products",
                fileName = "screenshot.png",
                contentType = "image/png",
            )

            val expectedResult = PresignedUrlResult(
                uploadUrl = "https://minio.example.com/products/uuid-456.png?presigned",
                objectKey = "uuid-456.png",
            )

            every {
                productDomainService.generatePresignedUrl(
                    bucket = command.bucket,
                    fileName = command.fileName,
                    contentType = command.contentType,
                )
            } returns expectedResult

            val result = useCase.execute(command)

            Then("정상적으로 URL이 반환된다") {
                result.uploadUrl shouldBe expectedResult.uploadUrl
                result.objectKey shouldBe expectedResult.objectKey
            }
        }

        When("지원하는 contentType(image/webp)으로 요청하면") {
            val command = GetPresignedUrlCommand(
                bucket = "products",
                fileName = "photo.webp",
                contentType = "image/webp",
            )

            val expectedResult = PresignedUrlResult(
                uploadUrl = "https://minio.example.com/products/uuid-789.webp?presigned",
                objectKey = "uuid-789.webp",
            )

            every {
                productDomainService.generatePresignedUrl(
                    bucket = command.bucket,
                    fileName = command.fileName,
                    contentType = command.contentType,
                )
            } returns expectedResult

            val result = useCase.execute(command)

            Then("정상적으로 URL이 반환된다") {
                result.uploadUrl shouldBe expectedResult.uploadUrl
                result.objectKey shouldBe expectedResult.objectKey
            }
        }

        When("지원하는 contentType(application/pdf)으로 요청하면") {
            val command = GetPresignedUrlCommand(
                bucket = "documents",
                fileName = "contract.pdf",
                contentType = "application/pdf",
            )

            val expectedResult = PresignedUrlResult(
                uploadUrl = "https://minio.example.com/documents/uuid-abc.pdf?presigned",
                objectKey = "uuid-abc.pdf",
            )

            every {
                productDomainService.generatePresignedUrl(
                    bucket = command.bucket,
                    fileName = command.fileName,
                    contentType = command.contentType,
                )
            } returns expectedResult

            val result = useCase.execute(command)

            Then("정상적으로 URL이 반환된다") {
                result.uploadUrl shouldBe expectedResult.uploadUrl
                result.objectKey shouldBe expectedResult.objectKey
            }
        }

        When("지원하지 않는 contentType(text/plain)으로 요청하면") {
            val command = GetPresignedUrlCommand(
                bucket = "products",
                fileName = "readme.txt",
                contentType = "text/plain",
            )

            every {
                productDomainService.generatePresignedUrl(
                    bucket = command.bucket,
                    fileName = command.fileName,
                    contentType = command.contentType,
                )
            } throws BusinessException(
                errorCode = ErrorCode.INVALID_FILE_TYPE,
                message = "지원하지 않는 파일 형식입니다: text/plain",
            )

            Then("INVALID_FILE_TYPE 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_FILE_TYPE
            }
        }

        When("지원하지 않는 contentType(application/zip)으로 요청하면") {
            val command = GetPresignedUrlCommand(
                bucket = "products",
                fileName = "archive.zip",
                contentType = "application/zip",
            )

            every {
                productDomainService.generatePresignedUrl(
                    bucket = command.bucket,
                    fileName = command.fileName,
                    contentType = command.contentType,
                )
            } throws BusinessException(
                errorCode = ErrorCode.INVALID_FILE_TYPE,
                message = "지원하지 않는 파일 형식입니다: application/zip",
            )

            Then("INVALID_FILE_TYPE 에러가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_FILE_TYPE
            }
        }
    }
})
