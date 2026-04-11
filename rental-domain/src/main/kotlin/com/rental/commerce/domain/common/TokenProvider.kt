package com.rental.commerce.domain.common

interface TokenProvider {

    fun createAccessToken(userId: Long, role: String): String
}
