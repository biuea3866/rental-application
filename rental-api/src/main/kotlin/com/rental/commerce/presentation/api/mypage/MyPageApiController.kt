package com.rental.commerce.presentation.api.mypage

import com.rental.commerce.application.user.GetMyPageUseCase
import com.rental.commerce.application.user.LenderProfileResponse
import com.rental.commerce.application.user.MyPageResponse
import com.rental.commerce.application.user.RenterProfileResponse
import com.rental.commerce.application.user.UpdateLenderProfileUseCase
import com.rental.commerce.application.user.UpdateRenterProfileUseCase
import com.rental.commerce.application.user.UpdateUserProfileUseCase
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/mypage")
class MyPageApiController(
    private val getMyPageUseCase: GetMyPageUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val updateLenderProfileUseCase: UpdateLenderProfileUseCase,
    private val updateRenterProfileUseCase: UpdateRenterProfileUseCase,
) {

    @GetMapping
    fun getMyPage(): ResponseEntity<MyPageResponse> {
        val userId = extractUserId()
        val response = getMyPageUseCase.execute(userId)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/profile")
    fun updateUserProfile(
        @RequestBody request: UpdateUserProfileRequest,
    ): ResponseEntity<Void> {
        val userId = extractUserId()
        updateUserProfileUseCase.execute(request.toCommand(userId))
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/lender-profile")
    fun updateLenderProfile(
        @RequestBody request: UpdateLenderProfileRequest,
    ): ResponseEntity<LenderProfileResponse> {
        val userId = extractUserId()
        val response = updateLenderProfileUseCase.execute(request.toCommand(userId))
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/renter-profile")
    fun updateRenterProfile(
        @RequestBody request: UpdateRenterProfileRequest,
    ): ResponseEntity<RenterProfileResponse> {
        val userId = extractUserId()
        val response = updateRenterProfileUseCase.execute(request.toCommand(userId))
        return ResponseEntity.ok(response)
    }

    private fun extractUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw com.rental.commerce.domain.common.UnauthorizedException()
        return (authentication.principal as? Long)
            ?: throw com.rental.commerce.domain.common.UnauthorizedException()
    }
}
