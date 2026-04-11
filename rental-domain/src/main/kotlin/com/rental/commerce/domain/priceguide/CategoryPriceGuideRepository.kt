package com.rental.commerce.domain.priceguide

interface CategoryPriceGuideRepository {

    fun findByCategoryCode(categoryCode: String): List<CategoryPriceGuide>
}
