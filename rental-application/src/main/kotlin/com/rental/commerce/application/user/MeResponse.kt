package com.rental.commerce.application.user

import com.rental.commerce.domain.user.User

// BLK-002: GET /api/v1/auth/me 응답 DTO
// FE AuthUser 타입과 맞춤: { id, email, name, role }
data class MeResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
) {
    companion object {
        fun from(user: User): MeResponse = MeResponse(
            id = user.requireId(),
            email = user.email,
            name = user.name,
            role = user.role.name,
        )
    }
}
