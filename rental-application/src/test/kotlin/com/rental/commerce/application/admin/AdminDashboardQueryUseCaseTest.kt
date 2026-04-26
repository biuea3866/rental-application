package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminDomainService
import com.rental.commerce.domain.admin.DashboardResult
import com.rental.commerce.domain.common.RentalStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AdminDashboardQueryUseCaseTest : BehaviorSpec({

    val adminDomainService = mockk<AdminDomainService>()
    val useCase = AdminDashboardQueryUseCase(adminDomainService)

    beforeEach {
        clearMocks(adminDomainService)
    }

    // ─────────────────────────────────────────────────────────────
    // execute() — 대여 상태별 카운트 집계
    // ─────────────────────────────────────────────────────────────

    Given("AdminDashboardQueryUseCase.execute() — 대여 상태별 카운트 집계") {

        When("REQUESTED 3건, APPROVED 2건, PAID 1건이 존재하면") {

            Then("adminDomainService.getDashboard()가 1회 호출되고 상태별 카운트를 반환한다") {
                val statusCounts = mapOf(
                    RentalStatus.REQUESTED to 3L,
                    RentalStatus.APPROVED to 2L,
                    RentalStatus.PAID to 1L,
                )
                every { adminDomainService.getDashboard() } returns DashboardResult(
                    totalRentals = 6L,
                    statusCounts = statusCounts,
                    revenue = 120_000L,
                )

                val result = useCase.execute()

                verify(exactly = 1) { adminDomainService.getDashboard() }
                result.totalRentals shouldBe 6L
                result.statusCounts[RentalStatus.REQUESTED] shouldBe 3L
                result.statusCounts[RentalStatus.APPROVED] shouldBe 2L
                result.statusCounts[RentalStatus.PAID] shouldBe 1L
                result.revenue shouldBe 120_000L
            }
        }

        When("대여가 전혀 없으면") {

            Then("totalRentals=0, statusCounts=빈Map, revenue=0이 반환된다") {
                every { adminDomainService.getDashboard() } returns DashboardResult(
                    totalRentals = 0L,
                    statusCounts = emptyMap(),
                    revenue = 0L,
                )

                val result = useCase.execute()

                result.totalRentals shouldBe 0L
                result.statusCounts shouldBe emptyMap()
                result.revenue shouldBe 0L
            }
        }
    }
})
