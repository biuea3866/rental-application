package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductSortBy
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import com.rental.commerce.domain.product.SortDirection

data class SearchProductCommand(
    val keyword: String? = null,
    val categoryCode: String? = null,
    val status: ProductStatus? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val rentalUnit: RentalUnit? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: ProductSortBy = ProductSortBy.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
)
