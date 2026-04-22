package com.rental.commerce.presentation.api.dispute

import com.rental.commerce.application.dispute.CancelDisputeUseCase
import com.rental.commerce.application.dispute.DisputeResult
import com.rental.commerce.application.dispute.GetDisputeUseCase
import com.rental.commerce.application.dispute.OpenDisputeUseCase
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper.Companion.HEADER_USER_ROLE
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * DisputeApiController (BE-402).
 *
 * - POST   /api/v1/disputes                — 대여자/등록자가 분쟁 오픈
 * - GET    /api/v1/disputes/{id}           — 당사자 또는 관리자 상세 조회
 * - POST   /api/v1/disputes/{id}/cancel    — opener 가 OPEN 상태에서만 취소
 *
 * 관리자 전용 엔드포인트(검토 시작/해결)는 AdminDisputeApiController 에서 BE-403 로 분리.
 */
@RestController
@RequestMapping("/api/v1")
class DisputeApiController(
    private val openDisputeUseCase: OpenDisputeUseCase,
    private val getDisputeUseCase: GetDisputeUseCase,
    private val cancelDisputeUseCase: CancelDisputeUseCase,
) {

    @PostMapping("/disputes")
    fun openDispute(
        @AuthenticatedMember userId: Long,
        @Valid @RequestBody request: OpenDisputeRequest,
    ): ResponseEntity<DisputeResult> {
        val result = openDisputeUseCase.execute(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping("/disputes/{id}")
    fun getDispute(
        @PathVariable id: Long,
        @AuthenticatedMember userId: Long,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<DisputeResult> {
        val isAdmin = httpRequest.getHeader(HEADER_USER_ROLE) == "ADMIN"
        val result = getDisputeUseCase.execute(id, userId, isAdmin)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/disputes/{id}/cancel")
    fun cancelDispute(
        @PathVariable id: Long,
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<DisputeResult> {
        val result = cancelDisputeUseCase.execute(id, userId)
        return ResponseEntity.ok(result)
    }
}
