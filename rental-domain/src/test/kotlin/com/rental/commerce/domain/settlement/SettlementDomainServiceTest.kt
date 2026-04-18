package com.rental.commerce.domain.settlement

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

class SettlementDomainServiceTest : BehaviorSpec({

    fun newMocks(): Pair<SettlementRepository, SettlementDomainService> {
        val repo = mockk<SettlementRepository>()
        return repo to SettlementDomainService(repo)
    }

    fun createSettlement(
        lenderId: Long = 1L,
        rentalId: Long = 10L,
        amount: BigDecimal = BigDecimal("50000"),
        commissionRate: BigDecimal = BigDecimal("0.10"),
    ) = Settlement.create(lenderId, rentalId, amount, commissionRate)

    // ─────────────────────────────────────────────────────────────
    // createSettlement — 정상 생성
    // ─────────────────────────────────────────────────────────────

    Given("createSettlement() — 정상 생성") {

        When("동일 rentalId에 정산이 존재하지 않으면") {
            val (repo, service) = newMocks()

            every { repo.findByRentalId(10L) } returns null
            every { repo.save(any()) } answers { firstArg() }

            val settlement = service.createSettlement(
                lenderId = 1L,
                rentalId = 10L,
                amount = BigDecimal("50000"),
                commissionRate = BigDecimal("0.10"),
            )

            Then("Settlement이 저장된다") {
                verify(exactly = 1) { repo.save(any()) }
            }

            Then("반환된 Settlement의 rentalId가 일치한다") {
                settlement.rentalId shouldBe 10L
            }

            Then("반환된 Settlement의 lenderId가 일치한다") {
                settlement.lenderId shouldBe 1L
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // createSettlement — 중복 정산 방지
    // ─────────────────────────────────────────────────────────────

    Given("createSettlement() — 중복 정산 방지") {

        When("동일 rentalId에 이미 정산이 존재하면") {
            val (repo, service) = newMocks()

            val existing = createSettlement(rentalId = 10L)
            every { repo.findByRentalId(10L) } returns existing

            Then("SettlementAlreadyExistsException이 발생한다") {
                shouldThrow<SettlementAlreadyExistsException> {
                    service.createSettlement(
                        lenderId = 1L,
                        rentalId = 10L,
                        amount = BigDecimal("50000"),
                        commissionRate = BigDecimal("0.10"),
                    )
                }
            }

            Then("save()는 호출되지 않는다") {
                verify(exactly = 0) { repo.save(any()) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getMySettlements — lenderId 기준 조회
    // ─────────────────────────────────────────────────────────────

    Given("getMySettlements() — lenderId 기준 조회") {

        When("lenderId=1인 등록자의 정산 내역이 2개 있으면") {
            val (repo, service) = newMocks()

            val settlements = listOf(
                createSettlement(rentalId = 10L),
                createSettlement(rentalId = 11L),
            )
            val pageQuery = PageQuery(page = 0, size = 20)

            every { repo.findByLenderId(1L, pageQuery) } returns PageResult(
                content = settlements,
                totalElements = 2L,
                totalPages = 1,
            )

            val result = service.getMySettlements(1L, pageQuery)

            Then("2개의 정산 내역이 반환된다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2L
            }
        }

        When("정산 내역이 없으면") {
            val (repo, service) = newMocks()

            val pageQuery = PageQuery(page = 0, size = 20)

            every { repo.findByLenderId(999L, pageQuery) } returns PageResult(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
            )

            val result = service.getMySettlements(999L, pageQuery)

            Then("빈 목록이 반환된다") {
                result.content.size shouldBe 0
                result.totalElements shouldBe 0L
            }
        }
    }
})
