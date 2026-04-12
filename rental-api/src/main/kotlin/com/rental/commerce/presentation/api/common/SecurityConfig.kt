package com.rental.commerce.presentation.api.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.commerce.domain.common.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.ZonedDateTime

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/price-guides/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/**").authenticated()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { exception ->
                exception
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                    .accessDeniedHandler(jwtAccessDeniedHandler())
            }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.addAllowedOriginPattern("*")
        configuration.addAllowedMethod("*")
        configuration.addAllowedHeader("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun jwtAuthenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _: AuthenticationException ->
            val errorCode = ErrorCode.UNAUTHORIZED
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
        }
    }

    @Bean
    fun jwtAccessDeniedHandler(): AccessDeniedHandler {
        return AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _ ->
            val errorCode = ErrorCode.FORBIDDEN
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
        }
    }
}
