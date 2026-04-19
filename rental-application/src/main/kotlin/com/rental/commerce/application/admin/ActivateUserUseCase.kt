package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ActivateUserUseCase — 관리자 사용자 활성화.
 *
 * - AdminDomainService.activateUser() 에 위임
 * - @Transactional 은 UseCase 에서만 선언
 */
@Service
@Transactional
class ActivateUserUseCase(
    private val adminDomainService: AdminDomainService,
) {

    fun execute(command: ActivateUserCommand) {
        adminDomainService.activateUser(command.userId)
    }
}
