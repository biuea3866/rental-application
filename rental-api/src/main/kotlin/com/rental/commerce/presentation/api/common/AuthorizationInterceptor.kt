package com.rental.commerce.presentation.api.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.commerce.domain.common.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.time.ZonedDateTime

@Component
class AuthorizationInterceptor(
    private val objectMapper: ObjectMapper,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true

        val method = handler as HandlerMethod

        // @Public 어노테이션이 있으면 인증 없이 통과
        if (method.hasMethodAnnotation(Public::class.java) ||
            method.beanType.isAnnotationPresent(Public::class.java)
        ) {
            return true
        }

        // JWT 파싱 성공 여부 확인 (외부 헤더 위조 방어)
        val authenticated = request.getAttribute(JwtAuthenticationFilter.ATTRIBUTE_AUTHENTICATED) as? Boolean
        if (authenticated != true) {
            return sendError(response, ErrorCode.UNAUTHORIZED)
        }

        // @RoleRequired 어노테이션 체크
        val roleRequired = method.getMethodAnnotation(RoleRequired::class.java)
            ?: method.beanType.getAnnotation(RoleRequired::class.java)

        if (roleRequired != null) {
            val userRole = request.getHeader(AuthenticatedRequestWrapper.HEADER_USER_ROLE)
            if (userRole !in roleRequired.roles) {
                return sendError(response, ErrorCode.FORBIDDEN)
            }
        }

        return true
    }

    private fun sendError(response: HttpServletResponse, errorCode: ErrorCode): Boolean {
        response.status = errorCode.httpStatus
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message,
            status = errorCode.httpStatus,
            timestamp = ZonedDateTime.now(),
        )
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
        return false
    }
}
