package com.rental.commerce.domain.user

import com.rental.commerce.domain.common.SocialProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SocialProviderTest : BehaviorSpec({

    Given("SocialProvider KAKAO") {
        val provider = SocialProvider.KAKAO

        When("providesPhoneNumber를 호출하면") {
            Then("true를 반환한다 (카카오는 전화번호 제공)") {
                provider.providesPhoneNumber() shouldBe true
            }
        }
    }

    Given("SocialProvider NAVER") {
        val provider = SocialProvider.NAVER

        When("providesPhoneNumber를 호출하면") {
            Then("true를 반환한다 (네이버는 전화번호 제공)") {
                provider.providesPhoneNumber() shouldBe true
            }
        }
    }

    Given("SocialProvider GOOGLE") {
        val provider = SocialProvider.GOOGLE

        When("providesPhoneNumber를 호출하면") {
            Then("false를 반환한다 (구글은 전화번호 미제공)") {
                provider.providesPhoneNumber() shouldBe false
            }
        }
    }

    Given("SocialProvider APPLE") {
        val provider = SocialProvider.APPLE

        When("providesPhoneNumber를 호출하면") {
            Then("false를 반환한다 (애플은 전화번호 미제공)") {
                provider.providesPhoneNumber() shouldBe false
            }
        }
    }
})
