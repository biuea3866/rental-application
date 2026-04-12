package com.rental.commerce.domain.product

enum class ProductSortBy {
    CREATED_AT,
    NAME,
    DEPOSIT_AMOUNT,
}

enum class SortDirection {
    ASC,
    DESC,
}

data class ProductSearchCondition(
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
