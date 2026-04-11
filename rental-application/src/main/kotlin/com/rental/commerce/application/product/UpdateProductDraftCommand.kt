package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.RentalUnit

data class UpdateProductDraftCommand(
    val userId: Long,
    val productId: Long,
    val step: Int,
    val name: String? = null,
    val description: String? = null,
    val categoryCode: String? = null,
    val condition: ProductCondition? = null,
    val depositAmount: Long? = null,
    val prices: List<PriceCommand>? = null,
    val images: List<ImageCommand>? = null,
)

data class PriceCommand(
    val rentalUnit: RentalUnit,
    val priceAmount: Long,
)

data class ImageCommand(
    val objectKey: String,
    val originalFilename: String,
    val sortOrder: Int,
)
