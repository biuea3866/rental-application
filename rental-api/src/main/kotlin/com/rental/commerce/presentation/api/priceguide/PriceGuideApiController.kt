package com.rental.commerce.presentation.api.priceguide

import com.rental.commerce.application.priceguide.GetPriceGuideUseCase
import com.rental.commerce.application.priceguide.PriceGuideResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/price-guides")
class PriceGuideApiController(
    private val getPriceGuideUseCase: GetPriceGuideUseCase,
) {

    @GetMapping
    fun getPriceGuides(
        @RequestParam categoryCode: String,
    ): ResponseEntity<List<PriceGuideResponse>> {
        val result = getPriceGuideUseCase.execute(categoryCode)
        return ResponseEntity.ok(result)
    }
}
