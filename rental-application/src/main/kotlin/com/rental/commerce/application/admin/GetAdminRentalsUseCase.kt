package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * GetAdminRentalsUseCase — 관리자 전체 대여 목록 조회.
 *
 * - AdminDomainService.getAllRentals() 에 위임
 * - @Transactional(readOnly = true) 는 UseCase 에서만 선언
 */
@Service
@Transactional(readOnly = true)
class GetAdminRentalsUseCase(
    private val adminDomainService: AdminDomainService,
) {

    fun execute(command: GetAdminRentalsCommand): AdminRentalPageResponse {
        val pageResult = adminDomainService.getAllRentals(command.toFilter())
        return AdminRentalPageResponse.from(pageResult)
    }
}
