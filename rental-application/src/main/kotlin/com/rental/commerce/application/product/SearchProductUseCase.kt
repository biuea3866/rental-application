package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductSearchCondition
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchProductUseCase(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
) {

    @Transactional(readOnly = true)
    fun execute(condition: ProductSearchCondition): Page<ProductSummaryResponse> {
        val productPage = productRepository.search(condition)

        return productPage.map { product ->
            val images = productImageRepository.findByProductId(product.productId)
            ProductSummaryResponse.from(product, images)
        }
    }
}
