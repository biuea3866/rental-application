package com.rental.commerce.presentation.api.common

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.context.request.NativeWebRequest

class MemberIdArgumentResolverTest : BehaviorSpec({

    val resolver = MemberIdArgumentResolver()

    afterEach {
        SecurityContextHolder.clearContext()
    }

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

        When("SecurityContext에 인증 정보(principal=Long)가 있으면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val authentication = UsernamePasswordAuthenticationToken(42L, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
            SecurityContextHolder.setContext(SecurityContextImpl(authentication))
            val webRequest = mockk<NativeWebRequest>()

            Then("principal에서 userId를 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe 42L
            }
        }

        When("SecurityContext에 인증 정보(principal=String)가 있으면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            val authentication = UsernamePasswordAuthenticationToken("99", null, listOf(SimpleGrantedAuthority("ROLE_USER")))
            SecurityContextHolder.setContext(SecurityContextImpl(authentication))
            val webRequest = mockk<NativeWebRequest>()

            Then("String principal을 Long으로 변환하여 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe 99L
            }
        }

        When("SecurityContext에 인증 정보가 없고 필수 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            SecurityContextHolder.clearContext()
            val webRequest = mockk<NativeWebRequest>()

            Then("UNAUTHORIZED BusinessException을 던진다") {
                val exception = shouldThrow<BusinessException> {
                    resolver.resolveArgument(parameter, null, webRequest, null)
                }
                exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
            }
        }

        When("클라이언트가 X-Member-Id 헤더를 위조하더라도 SecurityContext에 인증 정보가 없으면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns false
            SecurityContextHolder.clearContext()
            // 헤더는 이제 읽지 않으므로 위조된 헤더는 무시됨
            val webRequest = mockk<NativeWebRequest>()

            Then("헤더는 무시하고 UNAUTHORIZED BusinessException을 던진다") {
                val exception = shouldThrow<BusinessException> {
                    resolver.resolveArgument(parameter, null, webRequest, null)
                }
                exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
            }
        }

        When("SecurityContext에 인증 정보가 없고 nullable 파라미터이면") {
            val parameter = mockk<MethodParameter>()
            every { parameter.isOptional } returns true
            SecurityContextHolder.clearContext()
            val webRequest = mockk<NativeWebRequest>()

            Then("null을 반환한다") {
                val result = resolver.resolveArgument(parameter, null, webRequest, null)
                result shouldBe null
            }
        }
    }
})
