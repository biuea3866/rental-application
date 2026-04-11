package com.rental.commerce.application.product

data class CreateProductDraftCommand(
    val userId: Long,
    val name: String? = null,
    val categoryCode: String? = null,
)
