package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.CreateProductDraftCommand

data class CreateProductDraftRequest(
    val name: String? = null,
    val categoryCode: String? = null,
) {
    fun toCommand(userId: Long): CreateProductDraftCommand = CreateProductDraftCommand(
        userId = userId,
        name = name,
        categoryCode = categoryCode,
    )
}
