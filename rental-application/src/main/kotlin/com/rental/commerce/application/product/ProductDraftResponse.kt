package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductStatus

data class ProductDraftResponse(
    val id: Long,
    val status: ProductStatus,
    val currentDraftStep: Int?,
    val name: String?,
    val categoryCode: String?,
) {
    companion object {
        fun from(product: Product): ProductDraftResponse = ProductDraftResponse(
            id = product.productId,
            status = product.status,
            currentDraftStep = product.currentDraftStep,
            name = product.name,
            categoryCode = product.categoryCode,
        )
    }
}
