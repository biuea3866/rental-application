package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateProductDraftUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional
    fun execute(command: CreateProductDraftCommand): ProductDraftResponse {
        val savedProduct = productDomainService.createDraft(
            userId = command.userId,
            name = command.name,
            categoryCode = command.categoryCode,
        )

        return ProductDraftResponse.from(savedProduct)
    }
}
