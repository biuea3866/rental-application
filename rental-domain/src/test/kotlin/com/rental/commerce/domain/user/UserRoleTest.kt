package com.rental.commerce.domain.user

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class UserRoleTest : BehaviorSpec({

    Given("UserRole LENDER") {
        val role = UserRole.LENDER

        When("hasLenderRole을 호출하면") {
            Then("true를 반환한다") {
                role.hasLenderRole() shouldBe true
            }
        }

        When("hasRenterRole을 호출하면") {
            Then("false를 반환한다") {
                role.hasRenterRole() shouldBe false
            }
        }
    }

    Given("UserRole RENTER") {
        val role = UserRole.RENTER

        When("hasLenderRole을 호출하면") {
            Then("false를 반환한다") {
                role.hasLenderRole() shouldBe false
            }
        }

        When("hasRenterRole을 호출하면") {
            Then("true를 반환한다") {
                role.hasRenterRole() shouldBe true
            }
        }
    }

    Given("UserRole BOTH") {
        val role = UserRole.BOTH

        When("hasLenderRole을 호출하면") {
            Then("true를 반환한다") {
                role.hasLenderRole() shouldBe true
            }
        }

        When("hasRenterRole을 호출하면") {
            Then("true를 반환한다") {
                role.hasRenterRole() shouldBe true
            }
        }
    }
})
