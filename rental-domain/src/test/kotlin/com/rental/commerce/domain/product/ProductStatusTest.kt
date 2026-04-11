package com.rental.commerce.domain.product

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ProductStatusTest : BehaviorSpec({

    Given("ProductStatus 전이 규칙 검증") {

        When("DRAFT 상태에서") {
            val status = ProductStatus.DRAFT

            Then("UNDER_REVIEW로 전이할 수 있다") {
                status.canTransitTo(ProductStatus.UNDER_REVIEW) shouldBe true
            }

            Then("APPROVED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.APPROVED) shouldBe false
            }

            Then("REJECTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.REJECTED) shouldBe false
            }

            Then("AVAILABLE로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.AVAILABLE) shouldBe false
            }

            Then("RENTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.RENTED) shouldBe false
            }

            Then("자기 자신으로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.DRAFT) shouldBe false
            }
        }

        When("UNDER_REVIEW 상태에서") {
            val status = ProductStatus.UNDER_REVIEW

            Then("APPROVED로 전이할 수 있다") {
                status.canTransitTo(ProductStatus.APPROVED) shouldBe true
            }

            Then("REJECTED로 전이할 수 있다") {
                status.canTransitTo(ProductStatus.REJECTED) shouldBe true
            }

            Then("DRAFT로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.DRAFT) shouldBe false
            }

            Then("AVAILABLE로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.AVAILABLE) shouldBe false
            }

            Then("RENTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.RENTED) shouldBe false
            }
        }

        When("APPROVED 상태에서") {
            val status = ProductStatus.APPROVED

            Then("AVAILABLE로 전이할 수 있다") {
                status.canTransitTo(ProductStatus.AVAILABLE) shouldBe true
            }

            Then("DRAFT로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.DRAFT) shouldBe false
            }

            Then("UNDER_REVIEW로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.UNDER_REVIEW) shouldBe false
            }

            Then("REJECTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.REJECTED) shouldBe false
            }

            Then("RENTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.RENTED) shouldBe false
            }
        }

        When("REJECTED 상태에서") {
            val status = ProductStatus.REJECTED

            Then("DRAFT로 전이할 수 있다") {
                status.canTransitTo(ProductStatus.DRAFT) shouldBe true
            }

            Then("UNDER_REVIEW로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.UNDER_REVIEW) shouldBe false
            }

            Then("APPROVED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.APPROVED) shouldBe false
            }

            Then("AVAILABLE로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.AVAILABLE) shouldBe false
            }

            Then("RENTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.RENTED) shouldBe false
            }
        }

        When("AVAILABLE 상태에서") {
            val status = ProductStatus.AVAILABLE

            Then("RENTED로 전이할 수 있다") {
                status.canTransitTo(ProductStatus.RENTED) shouldBe true
            }

            Then("DRAFT로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.DRAFT) shouldBe false
            }

            Then("UNDER_REVIEW로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.UNDER_REVIEW) shouldBe false
            }

            Then("APPROVED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.APPROVED) shouldBe false
            }

            Then("REJECTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.REJECTED) shouldBe false
            }
        }

        When("RENTED 상태에서") {
            val status = ProductStatus.RENTED

            Then("AVAILABLE로 전이할 수 있다") {
                status.canTransitTo(ProductStatus.AVAILABLE) shouldBe true
            }

            Then("DRAFT로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.DRAFT) shouldBe false
            }

            Then("UNDER_REVIEW로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.UNDER_REVIEW) shouldBe false
            }

            Then("APPROVED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.APPROVED) shouldBe false
            }

            Then("REJECTED로 전이할 수 없다") {
                status.canTransitTo(ProductStatus.REJECTED) shouldBe false
            }
        }
    }
})
