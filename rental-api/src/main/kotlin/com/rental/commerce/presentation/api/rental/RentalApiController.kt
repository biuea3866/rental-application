package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.ApproveRentalCommand
import com.rental.commerce.application.rental.ApproveRentalUseCase
import com.rental.commerce.application.rental.CancelRentalCommand
import com.rental.commerce.application.rental.CancelRentalUseCase
import com.rental.commerce.application.rental.GetMyRentalsCommand
import com.rental.commerce.application.rental.GetMyRentalsUseCase
import com.rental.commerce.application.rental.GetRentalDetailUseCase
import com.rental.commerce.application.rental.ProcessPaymentResult
import com.rental.commerce.application.rental.ProcessPaymentUseCase
import com.rental.commerce.application.rental.RentalDetailResponse
import com.rental.commerce.application.rental.RentalResponse
import com.rental.commerce.application.rental.RentalRole
import com.rental.commerce.application.rental.RentalSummaryResponse
import com.rental.commerce.application.rental.RejectRentalCommand
import com.rental.commerce.application.rental.RejectRentalUseCase
import com.rental.commerce.application.rental.RequestRentalUseCase
import com.rental.commerce.application.rental.ReturnRentalCommand
import com.rental.commerce.application.rental.ReturnRentalUseCase
import com.rental.commerce.application.rental.StartRentalCommand
import com.rental.commerce.application.rental.StartRentalUseCase
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class RentalApiController(
    private val requestRentalUseCase: RequestRentalUseCase,
    private val approveRentalUseCase: ApproveRentalUseCase,
    private val rejectRentalUseCase: RejectRentalUseCase,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val startRentalUseCase: StartRentalUseCase,
    private val returnRentalUseCase: ReturnRentalUseCase,
    private val cancelRentalUseCase: CancelRentalUseCase,
    private val getMyRentalsUseCase: GetMyRentalsUseCase,
    private val getRentalDetailUseCase: GetRentalDetailUseCase,
) {

    /**
     * POST /api/v1/rentals — 대여 신청
     */
    @PostMapping("/rentals")
    fun requestRental(
        @AuthenticatedMember userId: Long,
        @Valid @RequestBody request: RequestRentalRequest,
    ): ResponseEntity<RentalResponse> {
        val response = requestRentalUseCase.execute(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * PATCH /api/v1/rentals/{rentalId}/approve — 대여 승인 (등록자)
     */
    @PatchMapping("/rentals/{rentalId}/approve")
    fun approveRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<RentalResponse> {
        val response = approveRentalUseCase.execute(
            ApproveRentalCommand(rentalId = rentalId, lenderId = userId)
        )
        return ResponseEntity.ok(response)
    }

    /**
     * PATCH /api/v1/rentals/{rentalId}/reject — 대여 거절 (등록자)
     */
    @PatchMapping("/rentals/{rentalId}/reject")
    fun rejectRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
        @Valid @RequestBody request: RejectRentalRequest,
    ): ResponseEntity<RentalResponse> {
        val response = rejectRentalUseCase.execute(
            RejectRentalCommand(rentalId = rentalId, lenderId = userId, reason = request.reason!!)
        )
        return ResponseEntity.ok(response)
    }

    /**
     * POST /api/v1/rentals/{rentalId}/payment — 결제 처리 (대여자)
     */
    @PostMapping("/rentals/{rentalId}/payment")
    fun processPayment(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
        @Valid @RequestBody request: ProcessPaymentRequest,
    ): ResponseEntity<ProcessPaymentResult> {
        val response = processPaymentUseCase.execute(request.toCommand(rentalId, userId))
        return ResponseEntity.ok(response)
    }

    /**
     * PATCH /api/v1/rentals/{rentalId}/start — 대여 시작 (등록자, 배송 시작)
     */
    @PatchMapping("/rentals/{rentalId}/start")
    fun startRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<RentalResponse> {
        val response = startRentalUseCase.execute(
            StartRentalCommand(rentalId = rentalId, lenderId = userId)
        )
        return ResponseEntity.ok(response)
    }

    /**
     * PATCH /api/v1/rentals/{rentalId}/return — 반납 처리 (대여자)
     */
    @PatchMapping("/rentals/{rentalId}/return")
    fun returnRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<RentalResponse> {
        val response = returnRentalUseCase.execute(
            ReturnRentalCommand(rentalId = rentalId, renterId = userId)
        )
        return ResponseEntity.ok(response)
    }

    /**
     * DELETE /api/v1/rentals/{rentalId} — 대여 취소 (대여자 또는 등록자)
     */
    @DeleteMapping("/rentals/{rentalId}")
    fun cancelRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
        @Valid @RequestBody request: CancelRentalRequest,
    ): ResponseEntity<RentalResponse> {
        val response = cancelRentalUseCase.execute(
            CancelRentalCommand(rentalId = rentalId, userId = userId, reason = request.reason!!)
        )
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/v1/rentals — 내 대여 목록 조회
     */
    @GetMapping("/rentals")
    fun getMyRentals(
        @AuthenticatedMember userId: Long,
        @RequestParam(defaultValue = "RENTER") role: RentalRole,
    ): ResponseEntity<List<RentalSummaryResponse>> {
        val response = getMyRentalsUseCase.execute(
            GetMyRentalsCommand(userId = userId, role = role)
        )
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/v1/rentals/{rentalId} — 대여 상세 조회
     */
    @GetMapping("/rentals/{rentalId}")
    fun getRentalDetail(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<RentalDetailResponse> {
        val response = getRentalDetailUseCase.execute(rentalId = rentalId, userId = userId)
        return ResponseEntity.ok(response)
    }
}
