package com.rental.commerce.application.priceguide

import com.rental.commerce.domain.priceguide.CategoryPriceGuide

data class PriceGuideResponse(
    val categoryCode: String,
    val rentalUnit: String,
    val minPrice: Long,
    val maxPrice: Long,
) {
    companion object {
        fun from(entity: CategoryPriceGuide): PriceGuideResponse = PriceGuideResponse(
            categoryCode = entity.categoryCode,
            rentalUnit = entity.rentalUnit,
            minPrice = entity.minPrice,
            maxPrice = entity.maxPrice,
        )
    }
}
