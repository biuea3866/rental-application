package com.rental.commerce.presentation.api.admin

import com.rental.commerce.application.admin.ActivateUserCommand
import com.rental.commerce.application.admin.ActivateUserUseCase
import com.rental.commerce.application.admin.AdminDashboardResult
import com.rental.commerce.application.admin.AdminDashboardQueryUseCase
import com.rental.commerce.application.admin.AdminRentalPageResponse
import com.rental.commerce.application.admin.GetAdminRentalsCommand
import com.rental.commerce.application.admin.GetAdminRentalsUseCase
import com.rental.commerce.application.admin.SuspendUserCommand
import com.rental.commerce.application.admin.SuspendUserUseCase
import com.rental.commerce.domain.common.RentalStatus
import com.rental.commerce.presentation.api.common.RoleRequired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * AdminApiController — 관리자 전용 REST API.
 *
 * 엔드포인트:
 *  - GET  /api/v1/admin/dashboard                   : 대시보드 통계 조회
 *  - GET  /api/v1/admin/rentals                      : 전체 대여 목록 조회
 *  - POST /api/v1/admin/users/{userId}/suspend       : 사용자 정지
 *  - POST /api/v1/admin/users/{userId}/activate      : 사용자 활성화
 *
 * 인증: X-Member-Id 헤더 (AuthorizationInterceptor / JwtAuthenticationFilter 에서 검증)
 * BFF 직접 Client 호출 금지 — UseCase 경유 필수
 */
@RestController
@RequestMapping("/api/v1/admin")
@RoleRequired("ADMIN")
class AdminApiController(
    private val adminDashboardQueryUseCase: AdminDashboardQueryUseCase,
    private val getAdminRentalsUseCase: GetAdminRentalsUseCase,
    private val suspendUserUseCase: SuspendUserUseCase,
    private val activateUserUseCase: ActivateUserUseCase,
) {

    /**
     * 대시보드 통계 조회.
     *
     * 대여 상태별 카운트, 총 대여 건수, 누적 매출을 반환한다.
     */
    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<AdminDashboardResult> {
        val result = adminDashboardQueryUseCase.execute()
        return ResponseEntity.ok(result)
    }

    /**
     * 전체 대여 목록 조회 (관리자 필터 + 페이지네이션).
     *
     * @param page     페이지 번호 (0-based, 기본값 0)
     * @param size     페이지 크기 (기본값 20)
     * @param status   대여 상태 필터 (선택)
     * @param renterId 대여자 ID 필터 (선택)
     * @param lenderId 대여 등록자 ID 필터 (선택)
     */
    @GetMapping("/rentals")
    fun getRentals(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: RentalStatus?,
        @RequestParam(required = false) renterId: Long?,
        @RequestParam(required = false) lenderId: Long?,
    ): ResponseEntity<AdminRentalPageResponse> {
        val result = getAdminRentalsUseCase.execute(
            GetAdminRentalsCommand(
                status = status,
                renterId = renterId,
                lenderId = lenderId,
                page = page,
                size = size,
            ),
        )
        return ResponseEntity.ok(result)
    }

    /**
     * 사용자 정지.
     *
     * @param userId 정지할 사용자 ID
     */
    @PostMapping("/users/{userId}/suspend")
    fun suspendUser(
        @PathVariable userId: Long,
    ): ResponseEntity<Unit> {
        suspendUserUseCase.execute(SuspendUserCommand(userId = userId))
        return ResponseEntity.ok().build()
    }

    /**
     * 사용자 활성화.
     *
     * @param userId 활성화할 사용자 ID
     */
    @PostMapping("/users/{userId}/activate")
    fun activateUser(
        @PathVariable userId: Long,
    ): ResponseEntity<Unit> {
        activateUserUseCase.execute(ActivateUserCommand(userId = userId))
        return ResponseEntity.ok().build()
    }
}
