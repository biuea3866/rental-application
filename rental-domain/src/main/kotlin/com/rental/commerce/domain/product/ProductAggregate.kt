package com.rental.commerce.domain.product

data class ProductAggregate(
    val product: Product,
    val prices: List<ProductPrice>,
    val images: List<ProductImage>,
)
