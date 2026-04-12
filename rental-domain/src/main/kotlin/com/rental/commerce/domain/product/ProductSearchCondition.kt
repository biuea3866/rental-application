package com.rental.commerce.domain.product

data class ProductSearchCondition(
    val keyword: String? = null,
    val categoryCode: String? = null,
    val status: ProductStatus? = ProductStatus.AVAILABLE,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val rentalUnit: RentalUnit? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "createdAt",
    val sortDirection: String = "DESC",
)
