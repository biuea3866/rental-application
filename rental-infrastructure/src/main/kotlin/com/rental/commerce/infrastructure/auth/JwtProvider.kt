package com.rental.commerce.infrastructure.auth

import com.rental.commerce.domain.common.ExpiredTokenException
import com.rental.commerce.domain.common.InvalidTokenException
import com.rental.commerce.domain.common.TokenProvider
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.security.PrivateKey
import java.security.PublicKey
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
    private val privateKey: PrivateKey,
    private val publicKey: PublicKey,
) : TokenProvider {

    companion object {
        private const val CLAIM_USER_ID = "userId"
        private const val CLAIM_ROLE = "role"
        private val ZONE_ID = ZoneId.of("Asia/Seoul")
    }

    override fun createAccessToken(userId: Long, role: String): String {
        val now = ZonedDateTime.now(ZONE_ID)
        val expiration = now.plus(jwtProperties.accessTokenExpiry)

        return Jwts.builder()
            .issuer(jwtProperties.issuer)
            .subject(userId.toString())
            .claim(CLAIM_USER_ID, userId)
            .claim(CLAIM_ROLE, role)
            .issuedAt(Date.from(now.toInstant()))
            .expiration(Date.from(expiration.toInstant()))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()
    }

    fun validateAccessToken(token: String): JwtClaims {
        if (token.isBlank()) {
            throw InvalidTokenException("토큰이 비어있습니다")
        }

        try {
            val claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(jwtProperties.issuer)
                .build()
                .parseSignedClaims(token)
                .payload

            val userId = claims[CLAIM_USER_ID, java.lang.Long::class.java]?.toLong()
                ?: throw InvalidTokenException("토큰에 userId가 없습니다")
            val role = claims[CLAIM_ROLE, String::class.java]
                ?: throw InvalidTokenException("토큰에 role이 없습니다")
            val issuedAt = claims.issuedAt?.toInstant()?.atZone(ZONE_ID)
                ?: throw InvalidTokenException("토큰에 issuedAt이 없습니다")
            val expiresAt = claims.expiration?.toInstant()?.atZone(ZONE_ID)
                ?: throw InvalidTokenException("토큰에 expiresAt이 없습니다")

            return JwtClaims(
                userId = userId,
                role = role,
                issuedAt = issuedAt,
                expiresAt = expiresAt,
            )
        } catch (exception: ExpiredJwtException) {
            throw ExpiredTokenException("토큰이 만료되었습니다")
        } catch (exception: JwtException) {
            throw InvalidTokenException("유효하지 않은 토큰입니다: ${exception.message}")
        } catch (exception: InvalidTokenException) {
            throw exception
        } catch (exception: ExpiredTokenException) {
            throw exception
        } catch (exception: IllegalArgumentException) {
            throw InvalidTokenException("토큰 파싱에 실패했습니다: ${exception.message}")
        }
    }
}
