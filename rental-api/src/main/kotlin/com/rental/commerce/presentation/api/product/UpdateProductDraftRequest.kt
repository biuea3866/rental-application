package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.ImageCommand
import com.rental.commerce.application.product.PriceCommand
import com.rental.commerce.application.product.UpdateProductDraftCommand
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.RentalUnit
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateProductDraftRequest(
    @field:Min(value = 1, message = "step은 1 이상이어야 합니다")
    @field:Max(value = 10, message = "step은 10 이하여야 합니다")
    val step: Int,

    val name: String? = null,
    val description: String? = null,
    val categoryCode: String? = null,
    val condition: ProductCondition? = null,
    val depositAmount: Long? = null,
    val prices: List<PriceRequest>? = null,
    val images: List<ImageRequest>? = null,
) {

    data class PriceRequest(
        val rentalUnit: RentalUnit,
        val priceAmount: Long,
    )

    data class ImageRequest(
        val objectKey: String,
        val originalFilename: String,
        val sortOrder: Int,
    )

    fun toCommand(userId: Long, productId: Long): UpdateProductDraftCommand = UpdateProductDraftCommand(
        userId = userId,
        productId = productId,
        step = step,
        name = name,
        description = description,
        categoryCode = categoryCode,
        condition = condition,
        depositAmount = depositAmount,
        prices = prices?.map { PriceCommand(rentalUnit = it.rentalUnit, priceAmount = it.priceAmount) },
        images = images?.map {
            ImageCommand(
                objectKey = it.objectKey,
                originalFilename = it.originalFilename,
                sortOrder = it.sortOrder,
            )
        },
    )
}
