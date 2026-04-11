package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ProductPriceTest : BehaviorSpec({

    Given("ProductPrice 생성 - 유효한 가격") {

        When("양수 가격으로 생성하면") {
            val price = ProductPrice(
                productId = 1L,
                rentalUnit = RentalUnit.DAILY,
                priceAmount = 10_000L,
            )

            Then("정상적으로 생성된다") {
                price.productId shouldBe 1L
                price.rentalUnit shouldBe RentalUnit.DAILY
                price.priceAmount shouldBe 10_000L
            }
        }

        When("각 대여 단위별로 가격을 생성하면") {
            val dailyPrice = ProductPrice(
                productId = 1L,
                rentalUnit = RentalUnit.DAILY,
                priceAmount = 5_000L,
            )
            val monthlyPrice = ProductPrice(
                productId = 1L,
                rentalUnit = RentalUnit.MONTHLY,
                priceAmount = 100_000L,
            )
            val yearlyPrice = ProductPrice(
                productId = 1L,
                rentalUnit = RentalUnit.YEARLY,
                priceAmount = 1_000_000L,
            )

            Then("모두 정상적으로 생성된다") {
                dailyPrice.rentalUnit shouldBe RentalUnit.DAILY
                monthlyPrice.rentalUnit shouldBe RentalUnit.MONTHLY
                yearlyPrice.rentalUnit shouldBe RentalUnit.YEARLY
            }
        }

        When("최소 유효 가격(1)으로 생성하면") {
            Then("예외가 발생하지 않는다") {
                shouldNotThrow<BusinessException> {
                    ProductPrice(
                        productId = 1L,
                        rentalUnit = RentalUnit.DAILY,
                        priceAmount = 1L,
                    )
                }
            }
        }
    }

    Given("ProductPrice 생성 - 유효하지 않은 가격") {

        When("가격이 0인 경우") {
            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    ProductPrice(
                        productId = 1L,
                        rentalUnit = RentalUnit.DAILY,
                        priceAmount = 0L,
                    )
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("가격이 음수인 경우") {
            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    ProductPrice(
                        productId = 1L,
                        rentalUnit = RentalUnit.MONTHLY,
                        priceAmount = -1_000L,
                    )
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("가격이 큰 음수인 경우") {
            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    ProductPrice(
                        productId = 1L,
                        rentalUnit = RentalUnit.YEARLY,
                        priceAmount = -999_999L,
                    )
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }
    }
})
