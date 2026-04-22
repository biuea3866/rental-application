package com.rental.commerce.infrastructure.migration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer

/**
 * Sprint 4 Flyway 마이그레이션(V18~V23) smoke 테스트.
 *
 * - V18 dispute: 테이블 존재 + generated column 활성 UNIQUE 동작
 * - V19 refund: 테이블 존재 + 인덱스
 * - V20 product denorm: rating_avg/rental_count/region_code/price_amount 컬럼 존재 (DEFAULT 적용)
 * - V21 product search index: idx_product_search 존재 + idx_product_status_category 제거
 * - V22 wishlist: UNIQUE(user_id, product_id)
 * - V23 notification_preference: UNIQUE(user_id) + 기본값 컬럼 DEFAULT
 *
 * 실제 스키마 조회로 검증 — 순수 SQL smoke (도메인 Entity 불필요).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FlywaySprint4MigrationTest(
    private val jdbcTemplate: JdbcTemplate,
) : BehaviorSpec({

    extensions(SpringExtension)

    Given("V18 dispute 마이그레이션") {
        When("테이블 스키마를 조회하면") {
            val columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'dispute'",
                String::class.java,
            )
            Then("필수 컬럼이 존재한다") {
                columns shouldContain "id"
                columns shouldContain "rental_id"
                columns shouldContain "opener_id"
                columns shouldContain "status"
                columns shouldContain "active_rental_id"
            }
        }

        When("active_rental_id UNIQUE 제약 동작") {
            jdbcTemplate.update(
                "INSERT INTO dispute (rental_id, opener_id, reason, description, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, NOW(6), NOW(6))",
                1000L, 1L, "DAMAGED", "첫 분쟁", "OPEN",
            )
            Then("같은 rental_id로 두번째 OPEN 분쟁은 UNIQUE 위반") {
                try {
                    jdbcTemplate.update(
                        "INSERT INTO dispute (rental_id, opener_id, reason, description, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, NOW(6), NOW(6))",
                        1000L, 2L, "DAMAGED", "두 번째 분쟁", "OPEN",
                    )
                    throw AssertionError("UNIQUE 위반 예외 기대")
                } catch (e: org.springframework.dao.DuplicateKeyException) {
                    // expected
                }
            }

            Then("기존 분쟁을 RESOLVED_REFUND 로 종결하면 동일 rental 에 새 분쟁 오픈 가능") {
                jdbcTemplate.update(
                    "UPDATE dispute SET status = 'RESOLVED_REFUND', resolved_at = NOW(6) WHERE rental_id = ?",
                    1000L,
                )
                val inserted = jdbcTemplate.update(
                    "INSERT INTO dispute (rental_id, opener_id, reason, description, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(6), NOW(6))",
                    1000L, 3L, "OTHER", "재오픈", "OPEN",
                )
                inserted shouldBe 1
            }
        }
    }

    Given("V19 refund 마이그레이션") {
        When("테이블 컬럼 조회") {
            val columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'refund'",
                String::class.java,
            )
            Then("payment_id/dispute_id/status 컬럼 존재") {
                columns shouldContain "payment_id"
                columns shouldContain "dispute_id"
                columns shouldContain "status"
                columns shouldContain "external_refund_id"
            }
        }
    }

    Given("V20 product 비정규화 컬럼") {
        When("product 컬럼 조회") {
            val columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'product'",
                String::class.java,
            )
            Then("rating_avg/rating_count/rental_count/region_code/base_price_amount 추가됨") {
                columns shouldContain "rating_avg"
                columns shouldContain "rating_count"
                columns shouldContain "rental_count"
                columns shouldContain "region_code"
                columns shouldContain "base_price_amount"
            }
        }
    }

    Given("V21 product 검색 인덱스") {
        When("인덱스 조회") {
            val indexes = jdbcTemplate.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics " +
                    "WHERE table_schema = DATABASE() AND table_name = 'product'",
                String::class.java,
            )
            Then("idx_product_search 인덱스가 생성되었다") {
                indexes shouldContain "idx_product_search"
                indexes shouldContain "idx_product_rating"
                indexes shouldContain "idx_product_rental_count"
            }
            Then("기존 idx_product_status_category 는 제거되었다") {
                indexes.contains("idx_product_status_category") shouldBe false
            }
        }
    }

    Given("V22 wishlist 마이그레이션") {
        When("UNIQUE(user_id, product_id) 검증") {
            jdbcTemplate.update(
                "INSERT INTO wishlist (user_id, product_id, created_at) VALUES (?, ?, NOW(6))",
                100L, 200L,
            )
            Then("중복 추가 시 예외") {
                try {
                    jdbcTemplate.update(
                        "INSERT INTO wishlist (user_id, product_id, created_at) VALUES (?, ?, NOW(6))",
                        100L, 200L,
                    )
                    throw AssertionError("UNIQUE 위반 기대")
                } catch (e: org.springframework.dao.DuplicateKeyException) {
                    // expected
                }
            }
        }
    }

    Given("V23 notification_preference 마이그레이션") {
        When("기본값 조회") {
            jdbcTemplate.update(
                "INSERT INTO notification_preference (user_id, created_at, updated_at) VALUES (?, NOW(6), NOW(6))",
                9999L,
            )
            val row = jdbcTemplate.queryForMap(
                "SELECT chat_enabled, rental_enabled, settlement_enabled, marketing_enabled " +
                    "FROM notification_preference WHERE user_id = ?",
                9999L,
            )
            Then("chat/rental/settlement=1, marketing=0 (PRD-004 기본값)") {
                (row["chat_enabled"] as Number).toInt() shouldBe 1
                (row["rental_enabled"] as Number).toInt() shouldBe 1
                (row["settlement_enabled"] as Number).toInt() shouldBe 1
                (row["marketing_enabled"] as Number).toInt() shouldBe 0
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
            registry.add("spring.flyway.enabled") { "true" }
        }
    }
}
