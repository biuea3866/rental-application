package com.rental.commerce.presentation.api.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ErrorResponseTest : BehaviorSpec({

    Given("ErrorResponse") {

        When("ErrorCode로 생성하면") {
            Then("code, message, status가 정확히 설정된다") {
                // TODO: ErrorResponse 구현 후 활성화
                // val response = ErrorResponse.of(ErrorCode.INVALID_INPUT)
                // response.code shouldBe "INVALID_INPUT"
                // response.message shouldBe ErrorCode.INVALID_INPUT.message
                // response.status shouldBe 400
            }
        }

        When("커스텀 메시지로 생성하면") {
            Then("커스텀 메시지가 반영된다") {
                // TODO
                // val response = ErrorResponse.of(ErrorCode.INVALID_INPUT, "이메일 형식이 올바르지 않습니다")
                // response.message shouldBe "이메일 형식이 올바르지 않습니다"
            }
        }
    }
})
