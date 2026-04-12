package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductStatus

data class ProductSubmitResponse(
    val productId: Long,
    val status: ProductStatus,
    val name: String,
    val description: String,
    val categoryCode: String,
    val condition: ProductCondition,
    val depositAmount: Long,
) {
    companion object {
        fun from(product: Product): ProductSubmitResponse = ProductSubmitResponse(
            productId = product.productId,
            status = product.status,
            name = requireNotNull(product.name) { "submit() 이후 name은 null일 수 없습니다" },
            description = requireNotNull(product.description) { "submit() 이후 description은 null일 수 없습니다" },
            categoryCode = requireNotNull(product.categoryCode) { "submit() 이후 categoryCode는 null일 수 없습니다" },
            condition = requireNotNull(product.condition) { "submit() 이후 condition은 null일 수 없습니다" },
            depositAmount = requireNotNull(product.depositAmount) { "submit() 이후 depositAmount는 null일 수 없습니다" },
        )
    }
}
