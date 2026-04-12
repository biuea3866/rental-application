package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductStatus
import java.time.ZonedDateTime

data class ProductSummaryResponse(
    val productId: Long,
    val name: String?,
    val categoryCode: String?,
    val status: ProductStatus,
    val depositAmount: Long?,
    val thumbnailUrl: String?,
    val createdAt: ZonedDateTime,
) {

    companion object {
        fun from(product: Product, images: List<ProductImage>): ProductSummaryResponse {
            val thumbnail = images
                .minByOrNull { it.sortOrder }
                ?.objectKey

            return ProductSummaryResponse(
                productId = product.productId,
                name = product.name,
                categoryCode = product.categoryCode,
                status = product.status,
                depositAmount = product.depositAmount,
                thumbnailUrl = thumbnail,
                createdAt = product.createdAt,
            )
        }
    }
}
