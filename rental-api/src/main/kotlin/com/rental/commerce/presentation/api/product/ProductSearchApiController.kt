package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.GetProductDetailUseCase
import com.rental.commerce.application.product.ProductDetailResponse
import com.rental.commerce.application.product.ProductSummaryResponse
import com.rental.commerce.application.product.SearchProductCommand
import com.rental.commerce.application.product.SearchProductUseCase
import com.rental.commerce.domain.product.ProductSortBy
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import com.rental.commerce.domain.product.SortDirection
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductSearchApiController(
    private val searchProductUseCase: SearchProductUseCase,
    private val getProductDetailUseCase: GetProductDetailUseCase,
) {

    @GetMapping
    fun searchProducts(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) status: ProductStatus?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(required = false) rentalUnit: RentalUnit?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "CREATED_AT") sortBy: ProductSortBy,
        @RequestParam(defaultValue = "DESC") sortDirection: SortDirection,
    ): ResponseEntity<Page<ProductSummaryResponse>> {
        val command = SearchProductCommand(
            keyword = keyword,
            categoryCode = category,
            status = status,
            minPrice = minPrice,
            maxPrice = maxPrice,
            rentalUnit = rentalUnit,
            page = page,
            size = size,
            sortBy = sortBy,
            sortDirection = sortDirection,
        )

        val result = searchProductUseCase.execute(command)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{productId}")
    fun getProductDetail(
        @PathVariable productId: Long,
    ): ResponseEntity<ProductDetailResponse> {
        val requestUserId = extractUserIdOrNull()
        val result = getProductDetailUseCase.execute(
            productId = productId,
            requestUserId = requestUserId,
        )
        return ResponseEntity.ok(result)
    }

    private fun extractUserIdOrNull(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return null
        return authentication.principal as? Long
    }
}
