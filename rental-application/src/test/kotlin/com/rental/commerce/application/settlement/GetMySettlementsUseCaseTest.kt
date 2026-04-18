package com.rental.commerce.application.settlement

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.settlement.Settlement
import com.rental.commerce.domain.settlement.SettlementDomainService
import com.rental.commerce.domain.settlement.SettlementStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

class GetMySettlementsUseCaseTest : BehaviorSpec({

    val settlementDomainService = mockk<SettlementDomainService>()
    val useCase = GetMySettlementsUseCase(settlementDomainService)

    fun createSettlement(
        lenderId: Long = 1L,
        rentalId: Long = 10L,
    ): Settlement = Settlement.create(
        lenderId = lenderId,
        rentalId = rentalId,
        amount = BigDecimal("50000"),
        commissionRate = BigDecimal("0.10"),
    )

    // ─────────────────────────────────────────────────────────────
    // execute — 정산 목록 조회
    // ─────────────────────────────────────────────────────────────

    Given("execute() — lenderId=1에 정산이 2개 있으면") {

        val settlements = listOf(
            createSettlement(lenderId = 1L, rentalId = 10L),
            createSettlement(lenderId = 1L, rentalId = 11L),
        )
        val pageQuery = PageQuery(page = 0, size = 20)
        val command = GetMySettlementsCommand(lenderId = 1L, pageQuery = pageQuery)

        every {
            settlementDomainService.getMySettlements(1L, pageQuery)
        } returns PageResult(
            content = settlements,
            totalElements = 2L,
            totalPages = 1,
        )

        When("execute를 호출하면") {
            val result = useCase.execute(command)

            Then("2개의 SettlementResult가 반환된다") {
                result.content shouldHaveSize 2
                result.totalElements shouldBe 2L
                result.totalPages shouldBe 1
            }

            Then("DomainService가 1회 호출된다") {
                verify(exactly = 1) { settlementDomainService.getMySettlements(1L, pageQuery) }
            }

            Then("SettlementResult의 lenderId가 일치한다") {
                result.content[0].lenderId shouldBe 1L
            }

            Then("SettlementResult의 rentalId가 일치한다") {
                result.content[0].rentalId shouldBe 10L
            }

            Then("netAmount가 올바르게 계산된다") {
                result.content[0].netAmount shouldBe BigDecimal("45000.00")
            }

            Then("status가 PENDING이다") {
                result.content[0].status shouldBe SettlementStatus.PENDING
            }
        }
    }

    Given("execute() — 정산 내역이 없으면") {

        val pageQuery = PageQuery(page = 0, size = 20)
        val command = GetMySettlementsCommand(lenderId = 999L, pageQuery = pageQuery)

        every {
            settlementDomainService.getMySettlements(999L, pageQuery)
        } returns PageResult(
            content = emptyList(),
            totalElements = 0L,
            totalPages = 0,
        )

        When("execute를 호출하면") {
            val result = useCase.execute(command)

            Then("빈 목록이 반환된다") {
                result.content shouldHaveSize 0
                result.totalElements shouldBe 0L
            }
        }
    }

    Given("execute() — pageQuery가 page=1, size=10이면") {

        val pageQuery = PageQuery(page = 1, size = 10)
        val command = GetMySettlementsCommand(lenderId = 1L, pageQuery = pageQuery)

        every {
            settlementDomainService.getMySettlements(1L, pageQuery)
        } returns PageResult(
            content = emptyList(),
            totalElements = 15L,
            totalPages = 2,
        )

        When("execute를 호출하면") {
            useCase.execute(command)

            Then("DomainService에 pageQuery가 그대로 전달된다") {
                verify(exactly = 1) { settlementDomainService.getMySettlements(1L, pageQuery) }
            }
        }
    }
})
