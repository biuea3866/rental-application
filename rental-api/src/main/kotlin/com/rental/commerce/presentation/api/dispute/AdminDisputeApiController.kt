package com.rental.commerce.presentation.api.dispute

import com.rental.commerce.application.dispute.DisputeResult
import com.rental.commerce.application.dispute.ResolveDisputeUseCase
import com.rental.commerce.application.dispute.StartDisputeReviewUseCase
import com.rental.commerce.presentation.api.common.RoleRequired
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * AdminDisputeApiController (BE-403).
 *
 * - PATCH /api/v1/admin/disputes/{id}/review    — OPEN → UNDER_REVIEW
 * - POST  /api/v1/admin/disputes/{id}/resolve   — UNDER_REVIEW → RESOLVED_{REFUND,PARTIAL,REJECTED}
 *
 * 권한: 관리자 전용 (AuthorizationInterceptor / X-Member-Role=ADMIN 으로 보호).
 * 해결 시 DisputeResolvedEvent 발행 → AFTER_COMMIT 리스너(BE-405)가 환불 처리.
 */
@RestController
@RequestMapping("/api/v1/admin/disputes")
@RoleRequired("ADMIN")
class AdminDisputeApiController(
    private val startDisputeReviewUseCase: StartDisputeReviewUseCase,
    private val resolveDisputeUseCase: ResolveDisputeUseCase,
) {

    @PatchMapping("/{id}/review")
    fun startReview(@PathVariable id: Long): ResponseEntity<DisputeResult> {
        return ResponseEntity.ok(startDisputeReviewUseCase.execute(id))
    }

    @PostMapping("/{id}/resolve")
    fun resolve(
        @PathVariable id: Long,
        @Valid @RequestBody request: ResolveDisputeRequest,
    ): ResponseEntity<DisputeResult> {
        return ResponseEntity.ok(resolveDisputeUseCase.execute(request.toCommand(id)))
    }
}
