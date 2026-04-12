package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductStatus

data class ProductSubmitResponse(
    val productId: Long,
    val status: ProductStatus,
    val name: String?,
    val description: String?,
    val categoryCode: String?,
    val condition: ProductCondition?,
    val depositAmount: Long?,
) {
    companion object {
        fun from(product: Product): ProductSubmitResponse = ProductSubmitResponse(
            productId = product.productId,
            status = product.status,
            name = product.name,
            description = product.description,
            categoryCode = product.categoryCode,
            condition = product.condition,
            depositAmount = product.depositAmount,
        )
    }
}
