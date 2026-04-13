package com.rental.commerce.presentation.api.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper

class AuthenticatedRequestWrapper(
    request: HttpServletRequest,
    private val userId: Long,
    private val role: String,
) : HttpServletRequestWrapper(request) {

    companion object {
        const val HEADER_USER_ID = "X-Member-Id"
        const val HEADER_USER_ROLE = "X-Member-Role"
    }

    override fun getHeader(name: String): String? = when (name) {
        HEADER_USER_ID -> userId.toString()
        HEADER_USER_ROLE -> role
        else -> super.getHeader(name)
    }

    override fun getHeaders(name: String): java.util.Enumeration<String> = when (name) {
        HEADER_USER_ID -> java.util.Collections.enumeration(listOf(userId.toString()))
        HEADER_USER_ROLE -> java.util.Collections.enumeration(listOf(role))
        else -> super.getHeaders(name)
    }

    override fun getHeaderNames(): java.util.Enumeration<String> {
        val names = mutableListOf<String>()
        val originalNames = super.getHeaderNames()
        while (originalNames.hasMoreElements()) {
            names.add(originalNames.nextElement())
        }
        if (!names.contains(HEADER_USER_ID)) names.add(HEADER_USER_ID)
        if (!names.contains(HEADER_USER_ROLE)) names.add(HEADER_USER_ROLE)
        return java.util.Collections.enumeration(names)
    }
}
