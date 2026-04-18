package com.rental.commerce.infrastructure.settlement

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.settlement.Settlement
import com.rental.commerce.domain.settlement.SettlementAlreadyExistsException
import com.rental.commerce.infrastructure.common.config.JpaAuditingConfig
import com.rental.commerce.infrastructure.settlement.mysql.SettlementJpaRepository
import com.rental.commerce.infrastructure.settlement.mysql.SettlementRepositoryImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import java.math.BigDecimal

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    value = [
        JpaAuditingConfig::class,
        SettlementRepositoryImpl::class,
    ],
)
@ActiveProfiles("test")
class SettlementRepositoryImplTest(
    private val settlementJpaRepository: SettlementJpaRepository,
    private val settlementRepositoryImpl: SettlementRepositoryImpl,
) : BehaviorSpec({

    extensions(SpringExtension)

    fun createAndSave(
        lenderId: Long = 1L,
        rentalId: Long = 10L,
        amount: BigDecimal = BigDecimal("50000"),
        commissionRate: BigDecimal = BigDecimal("0.10"),
    ): Settlement {
        val settlement = Settlement.create(lenderId, rentalId, amount, commissionRate)
        return settlementJpaRepository.save(settlement)
    }

    // ─────────────────────────────────────────────────────────────
    // save — 정산 저장
    // ─────────────────────────────────────────────────────────────

    Given("save() — 정산 저장") {

        When("유효한 정산을 저장하면") {
            val settlement = Settlement.create(1L, 100L, BigDecimal("50000"), BigDecimal("0.10"))
            val saved = settlementRepositoryImpl.save(settlement)

            Then("id가 자동 생성된다") {
                (saved.id > 0L) shouldBe true
            }

            Then("lenderId가 저장된다") {
                saved.lenderId shouldBe 1L
            }

            Then("rentalId가 저장된다") {
                saved.rentalId shouldBe 100L
            }

            Then("commission이 5000.00이다") {
                saved.commission shouldBe BigDecimal("5000.00")
            }

            Then("netAmount가 45000.00이다") {
                saved.netAmount shouldBe BigDecimal("45000.00")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // save — rental_id UNIQUE 제약 위반
    // ─────────────────────────────────────────────────────────────

    Given("save() — rental_id UNIQUE 제약 위반") {

        When("동일 rentalId로 정산을 두 번 저장하면") {
            createAndSave(rentalId = 200L)

            Then("DataIntegrityViolationException이 발생한다") {
                shouldThrow<DataIntegrityViolationException> {
                    createAndSave(rentalId = 200L)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByRentalId — 조회
    // ─────────────────────────────────────────────────────────────

    Given("findByRentalId() — 조회") {

        When("존재하는 rentalId로 조회하면") {
            createAndSave(rentalId = 300L)
            val result = settlementRepositoryImpl.findByRentalId(300L)

            Then("정산이 반환된다") {
                result.shouldNotBeNull()
                result.rentalId shouldBe 300L
            }
        }

        When("존재하지 않는 rentalId로 조회하면") {
            val result = settlementRepositoryImpl.findByRentalId(999999L)

            Then("null이 반환된다") {
                result.shouldBeNull()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByLenderId — 페이지네이션 조회
    // ─────────────────────────────────────────────────────────────

    Given("findByLenderId() — 페이지네이션 조회") {

        When("lenderId=10에 정산이 3개 있으면") {
            createAndSave(lenderId = 10L, rentalId = 401L)
            createAndSave(lenderId = 10L, rentalId = 402L)
            createAndSave(lenderId = 10L, rentalId = 403L)

            val result = settlementRepositoryImpl.findByLenderId(10L, PageQuery(page = 0, size = 20))

            Then("3개가 반환된다") {
                result.content.size shouldBe 3
                result.totalElements shouldBe 3L
            }
        }

        When("size=2, page=0으로 조회하면") {
            createAndSave(lenderId = 20L, rentalId = 501L)
            createAndSave(lenderId = 20L, rentalId = 502L)
            createAndSave(lenderId = 20L, rentalId = 503L)

            val result = settlementRepositoryImpl.findByLenderId(20L, PageQuery(page = 0, size = 2))

            Then("2개만 반환된다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 3L
                result.totalPages shouldBe 2
            }
        }

        When("정산이 없는 lenderId로 조회하면") {
            val result = settlementRepositoryImpl.findByLenderId(999L, PageQuery(page = 0, size = 20))

            Then("빈 목록이 반환된다") {
                result.content.size shouldBe 0
                result.totalElements shouldBe 0L
            }
        }
    }
}) {
    companion object {
        private val mysqlContainer = MySQLContainer("mysql:8.0").apply {
            withDatabaseName("rental_commerce_test")
            withUsername("test")
            withPassword("test")
        }

        init {
            mysqlContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { mysqlContainer.driverClassName }
        }
    }
}
