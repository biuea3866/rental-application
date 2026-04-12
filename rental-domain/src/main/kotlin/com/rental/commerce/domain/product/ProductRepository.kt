package com.rental.commerce.domain.product

import org.springframework.data.domain.Page

interface ProductRepository {

    fun save(product: Product): Product

    fun findById(productId: Long): Product?

    fun findByUserId(userId: Long): List<Product>

    fun search(condition: ProductSearchCondition): Page<Product>
}
