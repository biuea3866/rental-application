package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductPrice
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import java.time.ZonedDateTime

data class ProductDetailResponse(
    val id: Long,
    val userId: Long,
    val name: String?,
    val description: String?,
    val categoryCode: String?,
    val condition: ProductCondition?,
    val status: ProductStatus,
    val depositAmount: Long?,
    val prices: List<PriceResponse>,
    val images: List<ImageResponse>,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {

    data class PriceResponse(
        val id: Long,
        val rentalUnit: RentalUnit,
        val priceAmount: Long,
    ) {
        companion object {
            fun from(productPrice: ProductPrice): PriceResponse = PriceResponse(
                id = productPrice.productPriceId,
                rentalUnit = productPrice.rentalUnit,
                priceAmount = productPrice.priceAmount,
            )
        }
    }

    data class ImageResponse(
        val id: Long,
        val objectKey: String,
        val originalFilename: String,
        val sortOrder: Short,
    ) {
        companion object {
            fun from(productImage: ProductImage): ImageResponse = ImageResponse(
                id = productImage.productImageId,
                objectKey = productImage.objectKey,
                originalFilename = productImage.originalFilename,
                sortOrder = productImage.sortOrder,
            )
        }
    }

    companion object {
        fun from(
            product: Product,
            prices: List<ProductPrice>,
            images: List<ProductImage>,
        ): ProductDetailResponse = ProductDetailResponse(
            id = product.productId,
            userId = product.userId,
            name = product.name,
            description = product.description,
            categoryCode = product.categoryCode,
            condition = product.condition,
            status = product.status,
            depositAmount = product.depositAmount,
            prices = prices.map { PriceResponse.from(it) },
            images = images.map { ImageResponse.from(it) },
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
        )
    }
}
