package com.rental.commerce.presentation.api.common

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import jakarta.servlet.http.HttpServletRequest

class MemberIdArgumentResolverTest : BehaviorSpec({

    val resolver = MemberIdArgumentResolver()

    Given("supportsParameter") {

        When("@AuthenticatedMember 어노테이션이 있고 Long 타입인 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.hasParameterAnnotation(AuthenticatedMember::class.java) } returns true
            every { parameter.parameterType } returns Long::class.java

            Then("true를 반환한다") {
                resolver.supportsParameter(parameter) shouldBe true
            }
        }

        When("@AuthenticatedMember 어노테이션이 없는 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.hasParameterAnnotation(AuthenticatedMember::class.java) } returns false
            every { parameter.parameterType } returns Long::class.java

            Then("false를 반환한다") {
                resolver.supportsParameter(parameter) shouldBe false
            }
        }

        When("Long 타입이 아닌 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.hasParameterAnnotation(AuthenticatedMember::class.java) } returns true
            every { parameter.parameterType } returns String::class.java

            Then("false를 반환한다") {
                resolver.supportsParameter(parameter) shouldBe false
            }
        }
    }

    Given("resolveArgument") {

        When("request attribute에 X-Member-Id가 설정되어 있으면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val request = mockk<HttpServletRequest>()
            every { request.getAttribute("X-Member-Id") } returns 42L
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

            Then("attribute에서 userId를 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe 42L
            }
        }

        When("attribute는 없고 X-Member-Id 헤더가 설정되어 있으면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val request = mockk<HttpServletRequest>()
            every { request.getAttribute("X-Member-Id") } returns null
            every { request.getHeader("X-Member-Id") } returns "99"
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

            Then("헤더에서 userId를 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe 99L
            }
        }

        When("attribute도 헤더도 없고 필수 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val request = mockk<HttpServletRequest>()
            every { request.getAttribute("X-Member-Id") } returns null
            every { request.getHeader("X-Member-Id") } returns null
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

            Then("UNAUTHORIZED BusinessException을 던진다") {
                val exception = shouldThrow<BusinessException> {
                    resolver.resolveArgument(parameter, null, webRequest, null)
                }
                exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
            }
        }

        When("attribute도 헤더도 없고 nullable 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns true
            val request = mockk<HttpServletRequest>()
            every { request.getAttribute("X-Member-Id") } returns null
            every { request.getHeader("X-Member-Id") } returns null
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

            Then("null을 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe null
            }
        }

        When("attribute에 Number 타입(Int)이 설정되어 있으면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val request = mockk<HttpServletRequest>()
            every { request.getAttribute("X-Member-Id") } returns 7
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

            Then("Long으로 변환하여 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe 7L
            }
        }
    }
})
