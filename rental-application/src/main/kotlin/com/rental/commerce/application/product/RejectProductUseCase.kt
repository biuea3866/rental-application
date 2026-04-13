package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RejectProductUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional
    fun execute(command: RejectProductCommand) {
        productDomainService.reject(command.productId, command.reason)
    }
}
