package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.DeleteProductUseCase
import com.rental.commerce.application.product.GetMyProductsUseCase
import com.rental.commerce.application.product.MyProductSummaryResponse
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductManagementApiController(
    private val getMyProductsUseCase: GetMyProductsUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
) {

    @GetMapping("/api/v1/my-products")
    fun getMyProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<MyProductSummaryResponse>> {
        val userId = extractUserId()
        val result = getMyProductsUseCase.execute(
            userId = userId,
            page = page,
            size = size,
        )
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/api/v1/products/drafts/{productId}")
    fun deleteProduct(
        @PathVariable productId: Long,
    ): ResponseEntity<Void> {
        val userId = extractUserId()
        deleteProductUseCase.execute(userId = userId, productId = productId)
        return ResponseEntity.noContent().build()
    }

    private fun extractUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        return requireNotNull(authentication?.principal as? Long) {
            "인증 정보에서 사용자 ID를 추출할 수 없습니다"
        }
    }
}
