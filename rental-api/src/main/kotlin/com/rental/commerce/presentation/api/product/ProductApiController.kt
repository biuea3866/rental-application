package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.CreateProductDraftUseCase
import com.rental.commerce.application.product.GetProductDraftUseCase
import com.rental.commerce.application.product.ProductDraftDetailResponse
import com.rental.commerce.application.product.ProductDraftResponse
import com.rental.commerce.application.product.ProductSubmitResponse
import com.rental.commerce.application.product.SubmitProductCommand
import com.rental.commerce.application.product.SubmitProductUseCase
import com.rental.commerce.application.product.UpdateProductDraftUseCase
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products/drafts")
class ProductApiController(
    private val createProductDraftUseCase: CreateProductDraftUseCase,
    private val updateProductDraftUseCase: UpdateProductDraftUseCase,
    private val getProductDraftUseCase: GetProductDraftUseCase,
    private val submitProductUseCase: SubmitProductUseCase,
) {

    @PostMapping
    fun createDraft(
        @AuthenticatedMember userId: Long,
        @RequestBody request: CreateProductDraftRequest,
    ): ResponseEntity<ProductDraftResponse> {
        val response = createProductDraftUseCase.execute(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PatchMapping("/{productId}")
    fun updateDraft(
        @AuthenticatedMember userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: UpdateProductDraftRequest,
    ): ResponseEntity<ProductDraftResponse> {
        val response = updateProductDraftUseCase.execute(request.toCommand(userId, productId))
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{productId}")
    fun getDraft(
        @AuthenticatedMember userId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<ProductDraftDetailResponse> {
        val response = getProductDraftUseCase.execute(userId = userId, productId = productId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{productId}/submit")
    fun submitProduct(
        @AuthenticatedMember userId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<ProductSubmitResponse> {
        val command = SubmitProductCommand(userId = userId, productId = productId)
        val response = submitProductUseCase.execute(command)
        return ResponseEntity.ok(response)
    }
}
