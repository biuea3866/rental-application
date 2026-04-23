package com.rental.commerce.presentation.api.product

import com.rental.commerce.application.product.ApproveProductCommand
import com.rental.commerce.application.product.ApproveProductUseCase
import com.rental.commerce.application.product.RejectProductUseCase
import com.rental.commerce.presentation.api.common.RoleRequired
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/products")
@RoleRequired("ADMIN")
class AdminProductApiController(
    private val approveProductUseCase: ApproveProductUseCase,
    private val rejectProductUseCase: RejectProductUseCase,
) {

    @PatchMapping("/{productId}/approve")
    fun approve(
        @PathVariable productId: Long,
    ): ResponseEntity<Unit> {
        approveProductUseCase.execute(ApproveProductCommand(productId = productId))
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/{productId}/reject")
    fun reject(
        @PathVariable productId: Long,
        @Valid @RequestBody request: RejectProductRequest,
    ): ResponseEntity<Unit> {
        rejectProductUseCase.execute(request.toCommand(productId))
        return ResponseEntity.ok().build()
    }
}
