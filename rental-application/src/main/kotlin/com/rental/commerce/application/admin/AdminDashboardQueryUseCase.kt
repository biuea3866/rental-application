package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminDomainService
import com.rental.commerce.domain.admin.DashboardResult
import com.rental.commerce.domain.rental.RentalStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AdminDashboardResult(
    val totalRentals: Long,
    val statusCounts: Map<RentalStatus, Long>,
    val revenue: Long,
) {
    companion object {
        fun from(domain: DashboardResult): AdminDashboardResult = AdminDashboardResult(
            totalRentals = domain.totalRentals,
            statusCounts = domain.statusCounts,
            revenue = domain.revenue,
        )
    }
}

@Service
@Transactional(readOnly = true)
class AdminDashboardQueryUseCase(
    private val adminDomainService: AdminDomainService,
) {

    fun execute(): AdminDashboardResult {
        val dashboard = adminDomainService.getDashboard()
        return AdminDashboardResult.from(dashboard)
    }
}
