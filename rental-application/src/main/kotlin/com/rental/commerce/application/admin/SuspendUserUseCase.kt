package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * SuspendUserUseCase — 관리자 사용자 정지.
 *
 * - AdminDomainService.suspendUser() 에 위임
 * - @Transactional 은 UseCase 에서만 선언
 */
@Service
@Transactional
class SuspendUserUseCase(
    private val adminDomainService: AdminDomainService,
) {

    fun execute(command: SuspendUserCommand) {
        adminDomainService.suspendUser(command.userId)
    }
}
