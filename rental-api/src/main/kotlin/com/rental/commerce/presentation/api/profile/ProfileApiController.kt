package com.rental.commerce.presentation.api.profile

import com.rental.commerce.application.user.AddLenderProfileUseCase
import com.rental.commerce.application.user.AddRenterProfileCommand
import com.rental.commerce.application.user.AddRenterProfileUseCase
import com.rental.commerce.application.user.GetLenderProfileUseCase
import com.rental.commerce.application.user.GetRenterProfileUseCase
import com.rental.commerce.application.user.LenderProfileResponse
import com.rental.commerce.application.user.RenterProfileResponse
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class ProfileApiController(
    private val addLenderProfileUseCase: AddLenderProfileUseCase,
    private val addRenterProfileUseCase: AddRenterProfileUseCase,
    private val getLenderProfileUseCase: GetLenderProfileUseCase,
    private val getRenterProfileUseCase: GetRenterProfileUseCase,
) {

    @PostMapping("/lender-profile")
    fun createLenderProfile(
        @AuthenticatedMember userId: Long,
        @Valid @RequestBody request: CreateLenderProfileRequest,
    ): ResponseEntity<LenderProfileResponse> {
        val response = addLenderProfileUseCase.execute(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/renter-profile")
    fun createRenterProfile(
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<RenterProfileResponse> {
        val command = AddRenterProfileCommand(userId = userId)
        val response = addRenterProfileUseCase.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/lender-profile")
    fun getLenderProfile(
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<LenderProfileResponse> {
        val response = getLenderProfileUseCase.execute(userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/renter-profile")
    fun getRenterProfile(
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<RenterProfileResponse> {
        val response = getRenterProfileUseCase.execute(userId)
        return ResponseEntity.ok(response)
    }
}
