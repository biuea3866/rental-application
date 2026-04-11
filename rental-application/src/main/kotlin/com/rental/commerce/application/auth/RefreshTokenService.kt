package com.rental.commerce.application.auth

import com.rental.commerce.domain.common.InvalidTokenException
import com.rental.commerce.domain.common.RefreshTokenStore
import com.rental.commerce.domain.common.TokenFamilyCompromisedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenStore: RefreshTokenStore,
) {

    companion object {
        private const val REFRESH_TOKEN_EXPIRY_DAYS = 14L
    }

    fun issueRefreshToken(userId: Long): RefreshTokenResult {
        val tokenFamily = UUID.randomUUID().toString()
        val refreshToken = UUID.randomUUID().toString()

        refreshTokenStore.save(
            tokenFamily = tokenFamily,
            refreshToken = refreshToken,
            userId = userId,
            expiryDays = REFRESH_TOKEN_EXPIRY_DAYS,
        )

        return RefreshTokenResult(
            refreshToken = refreshToken,
            tokenFamily = tokenFamily,
            userId = userId,
        )
    }

    fun rotateToken(tokenFamily: String, currentRefreshToken: String): RefreshTokenResult {
        val storedData = refreshTokenStore.findByTokenFamily(tokenFamily)
            ?: throw InvalidTokenException("토큰 패밀리를 찾을 수 없습니다")

        // Family Detection: 이미 사용된 토큰이 재사용되면 탈취로 간주
        if (refreshTokenStore.isTokenUsed(tokenFamily, currentRefreshToken)) {
            refreshTokenStore.deleteByTokenFamily(tokenFamily)
            throw TokenFamilyCompromisedException("토큰 재사용이 감지되었습니다. 토큰 패밀리가 무효화되었습니다")
        }

        // 현재 저장된 토큰과 요청 토큰이 일치하는지 확인
        if (storedData.refreshToken != currentRefreshToken) {
            throw InvalidTokenException("유효하지 않은 리프레시 토큰입니다")
        }

        // 기존 토큰을 사용됨으로 마킹
        refreshTokenStore.markTokenAsUsed(tokenFamily, currentRefreshToken)

        // 새 토큰 생성 및 동일 family에 저장
        val newRefreshToken = UUID.randomUUID().toString()

        refreshTokenStore.save(
            tokenFamily = tokenFamily,
            refreshToken = newRefreshToken,
            userId = storedData.userId,
            expiryDays = REFRESH_TOKEN_EXPIRY_DAYS,
        )

        return RefreshTokenResult(
            refreshToken = newRefreshToken,
            tokenFamily = tokenFamily,
            userId = storedData.userId,
        )
    }

    fun revokeTokenFamily(tokenFamily: String) {
        refreshTokenStore.deleteByTokenFamily(tokenFamily)
    }
}

data class RefreshTokenResult(
    val refreshToken: String,
    val tokenFamily: String,
    val userId: Long,
)
