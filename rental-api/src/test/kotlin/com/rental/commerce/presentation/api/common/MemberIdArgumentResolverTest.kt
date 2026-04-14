package com.rental.commerce.presentation.api.common

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

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

        When("X-Member-Id 헤더에 유효한 userId가 있으면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val httpRequest = mockk<HttpServletRequest>()
            every { httpRequest.getHeader(AuthenticatedRequestWrapper.HEADER_USER_ID) } returns "42"
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest

            Then("헤더에서 userId를 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe 42L
            }
        }

        When("X-Member-Id 헤더가 없고 필수 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val httpRequest = mockk<HttpServletRequest>()
            every { httpRequest.getHeader(AuthenticatedRequestWrapper.HEADER_USER_ID) } returns null
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest

            Then("UNAUTHORIZED BusinessException을 던진다") {
                val exception = shouldThrow<BusinessException> {
                    resolver.resolveArgument(parameter, null, webRequest, null)
                }
                exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
            }
        }

        When("위조된 X-Member-Id 헤더는 JWT 검증을 통과하지 못하므로 헤더가 없는 것과 동일하다") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val httpRequest = mockk<HttpServletRequest>()
            every { httpRequest.getHeader(AuthenticatedRequestWrapper.HEADER_USER_ID) } returns null
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest

            Then("UNAUTHORIZED BusinessException을 던진다") {
                val exception = shouldThrow<BusinessException> {
                    resolver.resolveArgument(parameter, null, webRequest, null)
                }
                exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
            }
        }

        When("X-Member-Id 헤더가 없고 nullable 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns true
            val httpRequest = mockk<HttpServletRequest>()
            every { httpRequest.getHeader(AuthenticatedRequestWrapper.HEADER_USER_ID) } returns null
            val webRequest = mockk<NativeWebRequest>()
            every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest

            Then("null을 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe null
            }
        }
    }
})
