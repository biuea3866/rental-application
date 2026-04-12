package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateProductDraftUseCase(
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun execute(command: CreateProductDraftCommand): ProductDraftResponse {
        val product = Product(
            userId = command.userId,
            name = command.name,
            categoryCode = command.categoryCode,
            status = ProductStatus.DRAFT,
            currentDraftStep = 1,
        )

        val savedProduct = productRepository.save(product)

        return ProductDraftResponse.from(savedProduct)
    }
}
