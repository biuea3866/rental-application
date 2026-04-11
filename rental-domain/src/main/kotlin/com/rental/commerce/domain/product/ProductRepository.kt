package com.rental.commerce.domain.product

interface ProductRepository {

    fun save(product: Product): Product

    fun findById(productId: Long): Product?

    fun findByUserId(userId: Long): List<Product>
}
