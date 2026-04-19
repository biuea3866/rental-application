package com.rental.commerce.domain.priceguide

import org.springframework.stereotype.Service

@Service
class PriceGuideDomainService(
    private val categoryPriceGuideRepository: CategoryPriceGuideRepository,
) {

    fun findByCategoryCode(categoryCode: String): List<CategoryPriceGuide> =
        categoryPriceGuideRepository.findByCategoryCode(categoryCode)
}
