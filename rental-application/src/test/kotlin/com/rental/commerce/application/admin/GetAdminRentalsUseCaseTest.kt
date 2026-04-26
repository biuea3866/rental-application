package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminDomainService
import com.rental.commerce.domain.admin.AdminRentalResult
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.RentalStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class GetAdminRentalsUseCaseTest : BehaviorSpec({

    val adminDomainService = mockk<AdminDomainService>()
    val useCase = GetAdminRentalsUseCase(adminDomainService)

    beforeEach {
        clearMocks(adminDomainService)
    }

    // ─────────────────────────────────────────────────────────────
    // execute() — 전체 대여 목록 조회
    // ─────────────────────────────────────────────────────────────

    Given("GetAdminRentalsUseCase.execute()") {

        When("필터 없이 전체 조회하면") {
            Then("adminDomainService.getAllRentals()가 1회 호출되고 결과를 반환한다") {
                val now = ZonedDateTime.now()
                val rentalResult = AdminRentalResult(
                    rentalId = 1L,
                    renterId = 10L,
                    lenderId = 20L,
                    productId = 42L,
                    status = RentalStatus.REQUESTED,
                    totalAmount = 70_000L,
                    requestedAt = now,
                )
                every { adminDomainService.getAllRentals(any()) } returns PageResult(
                    content = listOf(rentalResult),
                    totalElements = 1L,
                    totalPages = 1,
                )

                val command = GetAdminRentalsCommand(page = 0, size = 20)
                val result = useCase.execute(command)

                verify(exactly = 1) { adminDomainService.getAllRentals(any()) }
                result.totalElements shouldBe 1L
                result.totalPages shouldBe 1
                result.content.size shouldBe 1
                result.content[0].rentalId shouldBe 1L
                result.content[0].status shouldBe RentalStatus.REQUESTED
            }
        }

        When("상태 필터로 REQUESTED 조회하면") {
            Then("adminDomainService.getAllRentals()에 status=REQUESTED 필터가 전달된다") {
                val now = ZonedDateTime.now()
                every { adminDomainService.getAllRentals(any()) } returns PageResult(
                    content = listOf(
                        AdminRentalResult(
                            rentalId = 2L,
                            renterId = 11L,
                            lenderId = 21L,
                            productId = 43L,
                            status = RentalStatus.REQUESTED,
                            totalAmount = 50_000L,
                            requestedAt = now,
                        ),
                    ),
                    totalElements = 1L,
                    totalPages = 1,
                )

                val command = GetAdminRentalsCommand(status = RentalStatus.REQUESTED)
                val result = useCase.execute(command)

                verify(exactly = 1) {
                    adminDomainService.getAllRentals(
                        match { it.status == RentalStatus.REQUESTED },
                    )
                }
                result.content[0].status shouldBe RentalStatus.REQUESTED
            }
        }

        When("대여가 없는 경우") {
            Then("빈 목록이 반환된다") {
                every { adminDomainService.getAllRentals(any()) } returns PageResult(
                    content = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )

                val result = useCase.execute(GetAdminRentalsCommand())

                result.content shouldBe emptyList()
                result.totalElements shouldBe 0L
                result.totalPages shouldBe 0
            }
        }
    }
})
