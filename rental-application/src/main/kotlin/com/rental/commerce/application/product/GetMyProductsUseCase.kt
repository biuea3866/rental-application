package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMyProductsUseCase(
    private val productRepository: ProductRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, page: Int, size: Int): Page<MyProductSummaryResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        return productRepository.findByUserIdAndStatusNot(
            userId = userId,
            excludeStatus = ProductStatus.DELETED,
            pageable = pageable,
        ).map { MyProductSummaryResponse.from(it) }
    }
}
