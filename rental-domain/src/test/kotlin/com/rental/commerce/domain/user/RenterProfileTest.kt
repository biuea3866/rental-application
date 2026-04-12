package com.rental.commerce.domain.user

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class RenterProfileTest : BehaviorSpec({

    Given("RenterProfile 생성 시") {

        When("기본값으로 생성하면") {
            val profile = RenterProfile(userId = 1L)

            Then("trustGrade는 BRONZE이다") {
                profile.trustGrade shouldBe TrustGrade.BRONZE
            }

            Then("totalTransactionCount는 0이다") {
                profile.totalTransactionCount shouldBe 0
            }
        }
    }

    Given("거래 횟수 증가 시") {

        When("incrementTransactionCount를 호출하면") {
            val profile = RenterProfile(userId = 1L)
            profile.incrementTransactionCount()

            Then("totalTransactionCount가 1 증가한다") {
                profile.totalTransactionCount shouldBe 1
            }
        }
    }

    Given("신뢰 등급 업그레이드 시") {

        When("거래 횟수가 10회에 도달하면") {
            val profile = RenterProfile(userId = 1L, totalTransactionCount = 9)
            profile.incrementTransactionCount()
            profile.upgradeTrustGrade()

            Then("trustGrade가 SILVER로 업그레이드된다") {
                profile.trustGrade shouldBe TrustGrade.SILVER
            }
        }

        When("거래 횟수가 10회 미만이면") {
            val profile = RenterProfile(userId = 1L, totalTransactionCount = 8)
            profile.incrementTransactionCount()
            profile.upgradeTrustGrade()

            Then("trustGrade는 BRONZE를 유지한다") {
                profile.trustGrade shouldBe TrustGrade.BRONZE
            }
        }

        When("거래 횟수가 50회에 도달하면") {
            val profile = RenterProfile(
                userId = 1L,
                totalTransactionCount = 49,
                trustGrade = TrustGrade.SILVER,
            )
            profile.incrementTransactionCount()
            profile.upgradeTrustGrade()

            Then("trustGrade가 GOLD로 업그레이드된다") {
                profile.trustGrade shouldBe TrustGrade.GOLD
            }
        }

        When("거래 횟수가 50회 미만이고 SILVER이면") {
            val profile = RenterProfile(
                userId = 1L,
                totalTransactionCount = 30,
                trustGrade = TrustGrade.SILVER,
            )
            profile.upgradeTrustGrade()

            Then("trustGrade는 SILVER를 유지한다") {
                profile.trustGrade shouldBe TrustGrade.SILVER
            }
        }

        When("이미 GOLD 등급이면") {
            val profile = RenterProfile(
                userId = 1L,
                totalTransactionCount = 100,
                trustGrade = TrustGrade.GOLD,
            )
            profile.upgradeTrustGrade()

            Then("trustGrade는 GOLD를 유지한다") {
                profile.trustGrade shouldBe TrustGrade.GOLD
            }
        }
    }

    Given("updateProfile") {

        When("배송지 주소를 변경하면") {
            val profile = RenterProfile(
                userId = 1L,
                shippingAddress = "서울시 강남구",
            )
            profile.updateProfile(shippingAddress = "서울시 서초구")

            Then("배송지 주소가 변경된다") {
                profile.shippingAddress shouldBe "서울시 서초구"
            }
        }

        When("null을 전달하면") {
            val profile = RenterProfile(
                userId = 1L,
                shippingAddress = "서울시 강남구",
            )
            profile.updateProfile(shippingAddress = null)

            Then("배송지 주소가 유지된다") {
                profile.shippingAddress shouldBe "서울시 강남구"
            }
        }
    }
})
