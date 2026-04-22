package com.rental.commerce.domain.product

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * ProductSearchCondition validation (BE-410).
 */
class ProductSearchConditionTest : BehaviorSpec({

    Given("ProductSearchCondition init 검증") {
        When("page < 0") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    ProductSearchCondition(page = -1)
                }
            }
        }
        When("size = 0") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    ProductSearchCondition(size = 0)
                }
            }
        }
        When("size > 100") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    ProductSearchCondition(size = 101)
                }
            }
        }
        When("minPrice > maxPrice") {
            Then("IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    ProductSearchCondition(minPrice = 5000L, maxPrice = 1000L)
                }
            }
        }
        When("신규 정렬 축 RATING_AVG") {
            Then("정상 설정") {
                val c = ProductSearchCondition(sortBy = ProductSortBy.RATING_AVG)
                c.sortBy shouldBe ProductSortBy.RATING_AVG
            }
        }
        When("RENTAL_COUNT 인기순 정렬") {
            Then("정상 설정") {
                val c = ProductSearchCondition(sortBy = ProductSortBy.RENTAL_COUNT)
                c.sortBy shouldBe ProductSortBy.RENTAL_COUNT
            }
        }
        When("categoryCodes 다중 필터") {
            Then("정상 설정") {
                val c = ProductSearchCondition(categoryCodes = listOf("A", "B"))
                c.categoryCodes shouldBe listOf("A", "B")
            }
        }
        When("regionCode 필터") {
            Then("정상 설정") {
                val c = ProductSearchCondition(regionCode = "SEOUL_GANGNAM")
                c.regionCode shouldBe "SEOUL_GANGNAM"
            }
        }
        When("정상 기본값") {
            Then("예외 없음") {
                shouldNotThrow<IllegalArgumentException> { ProductSearchCondition() }
            }
        }
    }
})
