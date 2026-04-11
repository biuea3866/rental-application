package com.rental.commerce.presentation.api.image

import com.rental.commerce.application.product.GetPresignedUrlCommand
import com.rental.commerce.application.product.GetPresignedUrlUseCase
import com.rental.commerce.application.product.PresignedUrlResponse
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ImageApiControllerTest : BehaviorSpec({

    val getPresignedUrlUseCase = mockk<GetPresignedUrlUseCase>()
    val controller = ImageApiController(getPresignedUrlUseCase)
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    Given("POST /api/v1/images/presigned-url") {

        When("정상적인 요청을 보내면") {
            val response = PresignedUrlResponse(
                uploadUrl = "https://minio.example.com/products/uuid-123.jpg?presigned",
                objectKey = "uuid-123.jpg",
            )

            every {
                getPresignedUrlUseCase.execute(
                    GetPresignedUrlCommand(
                        bucket = "products",
                        fileName = "test.jpg",
                        contentType = "image/jpeg",
                    )
                )
            } returns response

            val result = mockMvc.post("/api/v1/images/presigned-url") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"test.jpg","contentType":"image/jpeg","bucket":"products"}"""
            }

            Then("200 OK와 uploadUrl, objectKey가 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.uploadUrl") { value(response.uploadUrl) }
                    jsonPath("$.objectKey") { value(response.objectKey) }
                }
            }
        }

        When("지원하지 않는 contentType으로 요청하면") {
            every {
                getPresignedUrlUseCase.execute(
                    GetPresignedUrlCommand(
                        bucket = "products",
                        fileName = "readme.txt",
                        contentType = "text/plain",
                    )
                )
            } throws BusinessException(
                errorCode = ErrorCode.INVALID_FILE_TYPE,
            )

            val result = mockMvc.post("/api/v1/images/presigned-url") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"readme.txt","contentType":"text/plain","bucket":"products"}"""
            }

            Then("400 INVALID_FILE_TYPE 에러가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_FILE_TYPE") }
                }
            }
        }

        When("fileName이 빈 문자열로 요청하면") {
            val result = mockMvc.post("/api/v1/images/presigned-url") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"","contentType":"image/jpeg","bucket":"products"}"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("contentType이 빈 문자열로 요청하면") {
            val result = mockMvc.post("/api/v1/images/presigned-url") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"test.jpg","contentType":"","bucket":"products"}"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("bucket이 빈 문자열로 요청하면") {
            val result = mockMvc.post("/api/v1/images/presigned-url") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"test.jpg","contentType":"image/jpeg","bucket":""}"""
            }

            Then("400 Validation Error가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }
})
