package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.ApproveRentalCommand
import com.rental.commerce.application.rental.ApproveRentalUseCase
import com.rental.commerce.application.rental.CancelRentalUseCase
import com.rental.commerce.application.rental.GetMyRentalsCommand
import com.rental.commerce.application.rental.GetMyRentalsUseCase
import com.rental.commerce.application.rental.GetRentalDetailCommand
import com.rental.commerce.application.rental.GetRentalDetailUseCase
import com.rental.commerce.application.rental.ProcessPaymentResult
import com.rental.commerce.application.rental.ProcessPaymentUseCase
import com.rental.commerce.application.rental.RejectRentalUseCase
import com.rental.commerce.application.rental.RentalDetailResult
import com.rental.commerce.application.rental.RentalSummaryResult
import com.rental.commerce.application.rental.RequestRentalResult
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

    @PostMapping("/rentals")
    fun requestRental(
        @AuthenticatedMember userId: Long,
        @Valid @RequestBody request: CreateRentalRequest,
    ): ResponseEntity<RequestRentalResult> {
        val result = requestRentalUseCase.execute(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @PatchMapping("/rentals/{rentalId}/approve")
    fun approveRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<Unit> {
        approveRentalUseCase.execute(ApproveRentalCommand(rentalId = rentalId, userId = userId))
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/rentals/{rentalId}/reject")
    fun rejectRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
        @Valid @RequestBody request: RejectRentalRequest,
    ): ResponseEntity<Unit> {
        rejectRentalUseCase.execute(request.toCommand(userId, rentalId))
        return ResponseEntity.ok().build()
    }

    @PostMapping("/rentals/{rentalId}/payment")
    fun processPayment(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
        @Valid @RequestBody request: ProcessPaymentRequest,
    ): ResponseEntity<ProcessPaymentResult> {
        val result = processPaymentUseCase.execute(request.toCommand(userId, rentalId))
        return ResponseEntity.ok(result)
    }

    @PatchMapping("/rentals/{rentalId}/start")
    fun startRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<Unit> {
        startRentalUseCase.execute(StartRentalCommand(rentalId = rentalId, userId = userId))
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/rentals/{rentalId}/return")
    fun returnRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<Unit> {
        returnRentalUseCase.execute(ReturnRentalCommand(rentalId = rentalId, userId = userId))
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/rentals/{rentalId}")
    fun cancelRental(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
        @Valid @RequestBody request: CancelRentalRequest,
    ): ResponseEntity<Unit> {
        cancelRentalUseCase.execute(request.toCommand(userId, rentalId))
        return ResponseEntity.ok().build()
    }

    @GetMapping("/my-rentals")
    fun getMyRentals(
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<RentalListResponse> {
        val results = getMyRentalsUseCase.execute(GetMyRentalsCommand(userId = userId))
        return ResponseEntity.ok(RentalListResponse(rentals = results))
    }

    @GetMapping("/rentals/{rentalId}")
    fun getRentalDetail(
        @AuthenticatedMember userId: Long,
        @PathVariable rentalId: Long,
    ): ResponseEntity<RentalDetailResult> {
        val result = getRentalDetailUseCase.execute(GetRentalDetailCommand(rentalId = rentalId, userId = userId))
        return ResponseEntity.ok(result)
    }
}

data class RentalListResponse(
    val rentals: List<RentalSummaryResult>,
)
