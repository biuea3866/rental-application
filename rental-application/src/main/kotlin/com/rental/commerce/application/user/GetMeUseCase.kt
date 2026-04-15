package com.rental.commerce.application.user

import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// BLK-002: GET /api/v1/auth/me — 현재 로그인한 사용자 기본 정보 조회
@Service
class GetMeUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): MeResponse {
        val user = userDomainService.findById(userId)
        return MeResponse.from(user)
    }
}
