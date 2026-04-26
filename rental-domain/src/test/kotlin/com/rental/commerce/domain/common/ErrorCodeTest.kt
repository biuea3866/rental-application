package com.rental.commerce.domain.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank

class ErrorCodeTest : BehaviorSpec({

    Given("ErrorCode enum") {
        When("모든 에러 코드를 조회하면") {
            val errorCodes = ErrorCode.entries

            Then("각 코드에 httpStatus, code, message가 존재한다") {
                errorCodes.forEach { errorCode ->
                    errorCode.httpStatus shouldNotBe null
                    errorCode.code.shouldNotBeBlank()
                    errorCode.message.shouldNotBeBlank()
                }
            }
        }

        When("INVALID_INPUT 에러 코드를 조회하면") {
            val errorCode = ErrorCode.INVALID_INPUT

            Then("400 상태 코드를 가진다") {
                errorCode.httpStatus shouldBe 400
            }
        }

        When("UNAUTHORIZED 에러 코드를 조회하면") {
            val errorCode = ErrorCode.UNAUTHORIZED

            Then("401 상태 코드를 가진다") {
                errorCode.httpStatus shouldBe 401
            }
        }

        When("FORBIDDEN 에러 코드를 조회하면") {
            val errorCode = ErrorCode.FORBIDDEN

            Then("403 상태 코드를 가진다") {
                errorCode.httpStatus shouldBe 403
            }
        }

        When("RESOURCE_NOT_FOUND 에러 코드를 조회하면") {
            val errorCode = ErrorCode.RESOURCE_NOT_FOUND

            Then("404 상태 코드를 가진다") {
                errorCode.httpStatus shouldBe 404
            }
        }

        When("CONFLICT 에러 코드를 조회하면") {
            val errorCode = ErrorCode.CONFLICT

            Then("409 상태 코드를 가진다") {
                errorCode.httpStatus shouldBe 409
            }
        }

        When("INTERNAL_SERVER_ERROR 에러 코드를 조회하면") {
            val errorCode = ErrorCode.INTERNAL_SERVER_ERROR

            Then("500 상태 코드를 가진다") {
                errorCode.httpStatus shouldBe 500
            }
        }

        When("REFUND_EXCEEDS_PAYMENT 에러 코드를 조회하면") {
            val errorCode = ErrorCode.REFUND_EXCEEDS_PAYMENT

            Then("400 상태 코드, code=REFUND_EXCEEDS_PAYMENT, 비어있지 않은 message를 가진다") {
                errorCode.httpStatus shouldBe 400
                errorCode.code shouldBe "REFUND_EXCEEDS_PAYMENT"
                errorCode.message.shouldNotBeBlank()
            }
        }
    }
})
