package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductStatus
import java.time.ZonedDateTime

data class MyProductSummaryResponse(
    val productId: Long,
    val name: String?,
    val categoryCode: String?,
    val condition: ProductCondition?,
    val status: ProductStatus,
    val depositAmount: Long?,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(product: Product): MyProductSummaryResponse = MyProductSummaryResponse(
            productId = product.productId,
            name = product.name,
            categoryCode = product.categoryCode,
            condition = product.condition,
            status = product.status,
            depositAmount = product.depositAmount,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
        )
    }
}
