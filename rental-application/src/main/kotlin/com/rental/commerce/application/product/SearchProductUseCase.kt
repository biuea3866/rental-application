package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductSearchCondition
import com.rental.commerce.domain.product.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchProductUseCase(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
) {

    @Transactional(readOnly = true)
    fun execute(command: SearchProductCommand): Page<ProductSummaryResponse> {
        val condition = ProductSearchCondition(
            keyword = command.keyword,
            categoryCode = command.categoryCode,
            status = command.status ?: ProductStatus.AVAILABLE,
            minPrice = command.minPrice,
            maxPrice = command.maxPrice,
            rentalUnit = command.rentalUnit,
            page = command.page,
            size = command.size,
            sortBy = command.sortBy,
            sortDirection = command.sortDirection,
        )

        val productPage = productRepository.search(condition)

        val productIds = productPage.content.map { it.productId }
        val imagesByProductId = productImageRepository.findByProductIdIn(productIds)
            .groupBy { it.productId }

        return productPage.map { product ->
            val images = imagesByProductId[product.productId] ?: emptyList()
            ProductSummaryResponse.from(product, images)
        }
    }
}
