package com.rental.commerce.application.priceguide

import com.rental.commerce.domain.priceguide.PriceGuideDomainService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetPriceGuideUseCase(
    private val priceGuideDomainService: PriceGuideDomainService,
) {

    @Cacheable(cacheNames = ["priceGuide"], key = "#categoryCode")
    fun execute(categoryCode: String): List<PriceGuideResponse> {
        return priceGuideDomainService.findByCategoryCode(categoryCode)
            .map { PriceGuideResponse.from(it) }
    }
}
