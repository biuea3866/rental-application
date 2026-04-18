package com.rental.commerce.domain.admin

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.user.SocialProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZonedDateTime

class AdminDomainServiceTest : BehaviorSpec({

    val adminQueryRepository = mockk<AdminQueryRepository>()
    val userRepository = mockk<UserRepository>()
    val service = AdminDomainService(adminQueryRepository, userRepository)

    beforeEach {
        clearMocks(adminQueryRepository, userRepository)
    }

    val now = ZonedDateTime.now()

    fun createUser(id: Long = 1L, isSuspended: Boolean = false): User = User(
        email = "user$id@test.com",
        name = "테스트유저",
        phone = "010-0000-000$id",
        passwordHash = "hashed",
        role = UserRole.RENTER,
        isSuspended = isSuspended,
        id = id,
    )

    Given("getDashboard 호출 시") {

        When("대여 상태별 count와 주별 매출이 존재하면") {
            Then("totalRentals, statusCounts, revenue를 담은 DashboardResult를 반환한다") {
                val statusCounts = mapOf(
                    RentalStatus.REQUESTED to 5L,
                    RentalStatus.APPROVED to 3L,
                    RentalStatus.PAID to 2L,
                )
                every { adminQueryRepository.countByStatus() } returns statusCounts
                every {
                    adminQueryRepository.getWeeklyRevenue(any(), any())
                } returns listOf(
                    WeeklyRevenueResult(
                        weekStart = now.minusDays(7),
                        totalRevenue = 100_000L,
                        rentalCount = 2L,
                    ),
                    WeeklyRevenueResult(
                        weekStart = now,
                        totalRevenue = 50_000L,
                        rentalCount = 1L,
                    ),
                )

                val result = service.getDashboard()

                result.totalRentals shouldBe 10L
                result.statusCounts shouldBe statusCounts
                result.revenue shouldBe 150_000L
            }
        }

        When("대여가 없으면") {
            Then("totalRentals=0, revenue=0인 DashboardResult를 반환한다") {
                every { adminQueryRepository.countByStatus() } returns emptyMap()
                every { adminQueryRepository.getWeeklyRevenue(any(), any()) } returns emptyList()

                val result = service.getDashboard()

                result.totalRentals shouldBe 0L
                result.revenue shouldBe 0L
            }
        }
    }

    Given("getAllRentals 호출 시") {

        When("필터와 페이지 조건을 전달하면") {
            Then("AdminRentalResult 목록이 PageResult로 반환된다") {
                val filter = AdminRentalFilter(status = RentalStatus.REQUESTED, page = 0, size = 10)
                val rows = listOf(
                    AdminRentalRow(
                        rentalId = 1L,
                        renterId = 10L,
                        lenderId = 20L,
                        productId = 30L,
                        status = RentalStatus.REQUESTED,
                        totalAmount = 70_000L,
                        requestedAt = now,
                    ),
                )
                every { adminQueryRepository.findAllRentals(filter) } returns PageResult(
                    content = rows,
                    totalElements = 1L,
                    totalPages = 1,
                )

                val result = service.getAllRentals(filter)

                result.content.size shouldBe 1
                result.content[0].rentalId shouldBe 1L
                result.content[0].status shouldBe RentalStatus.REQUESTED
                result.totalElements shouldBe 1L
            }
        }
    }

    Given("suspendUser 호출 시") {

        When("존재하는 사용자 ID를 전달하면") {
            Then("해당 사용자를 정지 처리하고 저장한다") {
                val user = createUser(id = 42L)
                val savedSlot = slot<User>()

                every { userRepository.findById(42L) } returns user
                every { userRepository.save(capture(savedSlot)) } returns user

                service.suspendUser(42L)

                savedSlot.captured.isSuspended shouldBe true
                verify(exactly = 1) { userRepository.save(any()) }
            }
        }

        When("존재하지 않는 사용자 ID를 전달하면") {
            Then("USER_NOT_FOUND 예외가 발생한다") {
                every { userRepository.findById(999L) } returns null

                val ex = shouldThrow<BusinessException> {
                    service.suspendUser(999L)
                }
                ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }
    }

    Given("activateUser 호출 시") {

        When("정지된 사용자 ID를 전달하면") {
            Then("해당 사용자를 활성화하고 저장한다") {
                val user = createUser(id = 43L, isSuspended = true)
                val savedSlot = slot<User>()

                every { userRepository.findById(43L) } returns user
                every { userRepository.save(capture(savedSlot)) } returns user

                service.activateUser(43L)

                savedSlot.captured.isSuspended shouldBe false
                verify(exactly = 1) { userRepository.save(any()) }
            }
        }

        When("존재하지 않는 사용자 ID를 전달하면") {
            Then("USER_NOT_FOUND 예외가 발생한다") {
                every { userRepository.findById(998L) } returns null

                val ex = shouldThrow<BusinessException> {
                    service.activateUser(998L)
                }
                ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }
    }
})
