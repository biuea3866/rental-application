package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMyProductsUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, page: Int, size: Int): Page<MyProductSummaryResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        return productDomainService.getMyProducts(
            userId = userId,
            pageable = pageable,
        ).map { MyProductSummaryResponse.from(it) }
    }
}
