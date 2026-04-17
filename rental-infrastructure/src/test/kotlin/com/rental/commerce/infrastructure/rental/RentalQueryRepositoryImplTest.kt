package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalQueryCondition
import com.rental.commerce.domain.rental.RentalQueryRepository
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.infrastructure.common.config.JpaAuditingConfig
import com.rental.commerce.infrastructure.common.config.QuerydslConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import java.time.ZonedDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    value = [
        JpaAuditingConfig::class,
        QuerydslConfig::class,
        RentalQueryRepositoryImpl::class,
    ],
)
@ActiveProfiles("test")
class RentalQueryRepositoryImplTest(
    private val rentalJpaRepository: RentalJpaRepository,
    private val rentalPaymentJpaRepository: RentalPaymentJpaRepository,
    private val rentalQueryRepository: RentalQueryRepository,
) : BehaviorSpec({

    extensions(SpringExtension)

    val deliveryInfo = DeliveryInfo(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    val now = ZonedDateTime.now()

    fun saveRental(
        renterId: Long = 10L,
        lenderId: Long = 20L,
        status: RentalStatus = RentalStatus.REQUESTED,
    ): Rental {
        val rental = Rental.create(
            renterId = renterId,
            lenderId = lenderId,
            productId = 42L,
            startDate = now.plusDays(1),
            endDate = now.plusDays(8),
            totalAmount = 70_000L,
            depositAmount = 50_000L,
            deliveryInfo = deliveryInfo,
        )
        if (status == RentalStatus.APPROVED) rental.approve()
        return rentalJpaRepository.save(rental)
    }

    fun savePayment(rentalId: Long): RentalPayment {
        val payment = RentalPayment.create(
            rentalId = rentalId,
            amount = 70_000L,
            paymentMethod = PaymentMethod.CARD,
            orderId = "RC-$rentalId-1713063600000",
        )
        return rentalPaymentJpaRepository.save(payment)
    }

    // ===== findMyRentals =====

    Given("findMyRentals — 상태 필터 없이 전체 조회") {

        When("renterId로 대여를 조회하면") {
            Then("해당 userId가 renter인 대여만 반환된다") {
                val userId = 100L
                val rental1 = saveRental(renterId = userId, status = RentalStatus.REQUESTED)
                val rental2 = saveRental(renterId = userId, status = RentalStatus.APPROVED)
                saveRental(renterId = 999L, lenderId = 888L) // 다른 사람

                val condition = RentalQueryCondition(
                    userId = userId,
                    pageQuery = PageQuery(page = 0, size = 20),
                )
                val result = rentalQueryRepository.findMyRentals(condition)

                result.content shouldHaveSize 2
                result.totalElements shouldBe 2L
            }
        }

        When("lenderId로 대여를 조회하면") {
            Then("해당 userId가 lender인 대여도 반환된다") {
                val userId = 200L
                saveRental(renterId = userId)         // renter
                saveRental(lenderId = userId)          // lender
                saveRental(renterId = 888L, lenderId = 999L) // 무관

                val condition = RentalQueryCondition(
                    userId = userId,
                    pageQuery = PageQuery(page = 0, size = 20),
                )
                val result = rentalQueryRepository.findMyRentals(condition)

                result.content shouldHaveSize 2
            }
        }
    }

    Given("findMyRentals — 상태 필터 적용") {

        When("REQUESTED 상태 필터로 조회하면") {
            Then("REQUESTED 상태의 대여만 반환된다") {
                val userId = 300L
                saveRental(renterId = userId, status = RentalStatus.REQUESTED)
                saveRental(renterId = userId, status = RentalStatus.APPROVED)

                val condition = RentalQueryCondition(
                    userId = userId,
                    statusFilter = RentalStatus.REQUESTED,
                    pageQuery = PageQuery(page = 0, size = 20),
                )
                val result = rentalQueryRepository.findMyRentals(condition)

                result.content shouldHaveSize 1
                result.content[0].status shouldBe RentalStatus.REQUESTED
            }
        }

        When("APPROVED 상태 필터로 조회하면") {
            Then("APPROVED 상태의 대여만 반환된다") {
                val userId = 400L
                saveRental(renterId = userId, status = RentalStatus.REQUESTED)
                saveRental(renterId = userId, status = RentalStatus.APPROVED)

                val condition = RentalQueryCondition(
                    userId = userId,
                    statusFilter = RentalStatus.APPROVED,
                    pageQuery = PageQuery(page = 0, size = 20),
                )
                val result = rentalQueryRepository.findMyRentals(condition)

                result.content shouldHaveSize 1
                result.content[0].status shouldBe RentalStatus.APPROVED
            }
        }
    }

    Given("findMyRentals — 페이지네이션") {

        When("size=2, page=0으로 조회하면") {
            Then("첫 2건만 반환되고 totalElements는 전체 건수이다") {
                val userId = 500L
                repeat(5) { saveRental(renterId = userId) }

                val condition = RentalQueryCondition(
                    userId = userId,
                    pageQuery = PageQuery(page = 0, size = 2),
                )
                val result = rentalQueryRepository.findMyRentals(condition)

                result.content shouldHaveSize 2
                result.totalElements shouldBe 5L
                result.totalPages shouldBe 3
            }
        }

        When("결과가 없으면") {
            Then("빈 PageResult를 반환한다") {
                val userId = 999999L

                val condition = RentalQueryCondition(
                    userId = userId,
                    pageQuery = PageQuery(page = 0, size = 20),
                )
                val result = rentalQueryRepository.findMyRentals(condition)

                result.content shouldHaveSize 0
                result.totalElements shouldBe 0L
            }
        }
    }

    // ===== findRentalWithPayment =====

    Given("findRentalWithPayment — 대여 상세 + 결제 JOIN") {

        When("결제가 있는 대여를 조회하면") {
            Then("Rental과 RentalPayment가 함께 반환된다") {
                val rental = saveRental(renterId = 10L, lenderId = 20L)
                val payment = savePayment(rental.id)

                val result = rentalQueryRepository.findRentalWithPayment(rental.id)

                val notNullResult = result.shouldNotBeNull()
                notNullResult.rental.id shouldBe rental.id
                val notNullPayment = result.payment.shouldNotBeNull()
                notNullPayment.rentalId shouldBe rental.id
            }
        }

        When("결제가 없는 대여를 조회하면") {
            Then("payment가 null인 RentalWithPayment가 반환된다") {
                val rental = saveRental(renterId = 10L, lenderId = 20L)

                val result = rentalQueryRepository.findRentalWithPayment(rental.id)

                val notNullResult = result.shouldNotBeNull()
                notNullResult.rental.id shouldBe rental.id
                result.payment.shouldBeNull()
            }
        }

        When("존재하지 않는 rentalId로 조회하면") {
            Then("null을 반환한다") {
                val result = rentalQueryRepository.findRentalWithPayment(999999L)
                result.shouldBeNull()
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
