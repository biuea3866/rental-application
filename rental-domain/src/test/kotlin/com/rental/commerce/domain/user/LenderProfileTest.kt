package com.rental.commerce.domain.user

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class LenderProfileTest : BehaviorSpec({

    Given("LenderProfile 생성 시") {

        When("기본값으로 생성하면") {
            val profile = LenderProfile(userId = 1L)

            Then("lenderType은 INDIVIDUAL이다") {
                profile.lenderType shouldBe LenderType.INDIVIDUAL
            }

            Then("verificationStatus는 PENDING이다") {
                profile.verificationStatus shouldBe VerificationStatus.PENDING
            }

            Then("정산 계좌 정보는 null이다") {
                profile.settlementAccountBank shouldBe null
                profile.settlementAccountNumber shouldBe null
            }
        }
    }

    Given("정산 계좌 등록 시") {

        When("유효한 은행명과 계좌번호로 등록하면") {
            val profile = LenderProfile(userId = 1L)
            profile.registerSettlementAccount("신한은행", "110-123-456789")

            Then("정산 계좌 은행이 설정된다") {
                profile.settlementAccountBank shouldBe "신한은행"
            }

            Then("정산 계좌 번호가 설정된다") {
                profile.settlementAccountNumber shouldBe "110-123-456789"
            }
        }
    }

    Given("인증 상태 변경 시") {

        When("PENDING 상태에서 verify를 호출하면") {
            val profile = LenderProfile(userId = 1L)
            profile.verify()

            Then("verificationStatus가 VERIFIED로 변경된다") {
                profile.verificationStatus shouldBe VerificationStatus.VERIFIED
            }
        }
    }
})
