package com.rental.commerce.domain.product

interface ProductPriceRepository {

    fun findByProductId(productId: Long): List<ProductPrice>

    fun saveAll(prices: List<ProductPrice>): List<ProductPrice>

    fun deleteByProductId(productId: Long)
}
