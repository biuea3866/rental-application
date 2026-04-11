package com.rental.commerce.application.priceguide

import com.rental.commerce.domain.priceguide.CategoryPriceGuideRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetPriceGuideUseCase(
    private val categoryPriceGuideRepository: CategoryPriceGuideRepository,
) {

    @Cacheable(cacheNames = ["priceGuide"], key = "#categoryCode")
    fun execute(categoryCode: String): List<PriceGuideResponse> {
        return categoryPriceGuideRepository.findByCategoryCode(categoryCode)
            .map { PriceGuideResponse.from(it) }
    }
}
