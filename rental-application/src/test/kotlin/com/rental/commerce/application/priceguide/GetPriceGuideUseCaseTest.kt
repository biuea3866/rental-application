package com.rental.commerce.application.priceguide

import com.rental.commerce.domain.priceguide.CategoryPriceGuide
import com.rental.commerce.domain.priceguide.CategoryPriceGuideRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetPriceGuideUseCaseTest : BehaviorSpec({

    val categoryPriceGuideRepository = mockk<CategoryPriceGuideRepository>()
    val useCase = GetPriceGuideUseCase(categoryPriceGuideRepository)

    Given("카테고리 코드로 가이드 가격을 조회할 때") {

        When("해당 카테고리에 가이드 가격이 존재하면") {
            val categoryCode = "ELECTRONICS"
            val guides = listOf(
                CategoryPriceGuide(
                    categoryPriceGuideId = 1L,
                    categoryCode = "ELECTRONICS",
                    rentalUnit = "DAILY",
                    minPrice = 5000L,
                    maxPrice = 50000L,
                ),
                CategoryPriceGuide(
                    categoryPriceGuideId = 2L,
                    categoryCode = "ELECTRONICS",
                    rentalUnit = "MONTHLY",
                    minPrice = 50000L,
                    maxPrice = 500000L,
                ),
                CategoryPriceGuide(
                    categoryPriceGuideId = 3L,
                    categoryCode = "ELECTRONICS",
                    rentalUnit = "YEARLY",
                    minPrice = 500000L,
                    maxPrice = 5000000L,
                ),
            )

            every { categoryPriceGuideRepository.findByCategoryCode(categoryCode) } returns guides

            val result = useCase.execute(categoryCode)

            Then("가이드 가격 목록이 반환된다") {
                result shouldHaveSize 3
            }

            Then("각 응답에 카테고리 코드가 포함된다") {
                result.forEach { it.categoryCode shouldBe "ELECTRONICS" }
            }

            Then("DAILY 가이드 가격이 올바르게 매핑된다") {
                val daily = result.first { it.rentalUnit == "DAILY" }
                daily.minPrice shouldBe 5000L
                daily.maxPrice shouldBe 50000L
            }

            Then("MONTHLY 가이드 가격이 올바르게 매핑된다") {
                val monthly = result.first { it.rentalUnit == "MONTHLY" }
                monthly.minPrice shouldBe 50000L
                monthly.maxPrice shouldBe 500000L
            }

            Then("YEARLY 가이드 가격이 올바르게 매핑된다") {
                val yearly = result.first { it.rentalUnit == "YEARLY" }
                yearly.minPrice shouldBe 500000L
                yearly.maxPrice shouldBe 5000000L
            }

            Then("리포지토리가 정확히 한 번 호출된다") {
                verify(exactly = 1) { categoryPriceGuideRepository.findByCategoryCode(categoryCode) }
            }
        }

        When("해당 카테고리에 가이드 가격이 존재하지 않으면") {
            val categoryCode = "UNKNOWN"

            every { categoryPriceGuideRepository.findByCategoryCode(categoryCode) } returns emptyList()

            val result = useCase.execute(categoryCode)

            Then("빈 목록이 반환된다") {
                result shouldHaveSize 0
            }
        }
    }
})
