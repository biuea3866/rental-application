package com.rental.commerce.presentation.api.common

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import jakarta.servlet.http.HttpServletRequest

@Component
class MemberIdArgumentResolver : HandlerMethodArgumentResolver {

    companion object {
        const val MEMBER_ID_ATTRIBUTE = "X-Member-Id"
        const val MEMBER_ID_HEADER = "X-Member-Id"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticatedMember::class.java) &&
            parameter.parameterType == Long::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long? {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)

        val memberId = request?.getAttribute(MEMBER_ID_ATTRIBUTE)?.let { convertToLong(it) }
            ?: request?.getHeader(MEMBER_ID_HEADER)?.let { convertToLong(it) }

        if (memberId == null && !parameter.isOptional) {
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }

        return memberId
    }

    private fun convertToLong(value: Any): Long? {
        return when (value) {
            is Long -> value
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }
}
