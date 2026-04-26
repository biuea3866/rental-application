package com.rental.commerce.infrastructure.user

import com.rental.commerce.domain.common.SocialProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import com.rental.commerce.infrastructure.common.config.JpaAuditingConfig
import com.rental.commerce.infrastructure.common.config.QuerydslConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    value = [
        JpaAuditingConfig::class,
        QuerydslConfig::class,
        UserRepositoryImpl::class,
    ],
)
@ActiveProfiles("test")
class UserRepositoryImplTest(
    private val userRepository: UserRepository,
) : BehaviorSpec({

    extensions(SpringExtension)

    fun createUser(
        email: String = "test@example.com",
        name: String = "홍길동",
        phone: String = "010-1234-5678",
        passwordHash: String = "hashed_password",
        role: UserRole = UserRole.RENTER,
        socialProvider: SocialProvider? = null,
        socialProviderId: String? = null,
    ): User {
        return User(
            email = email,
            name = name,
            phone = phone,
            passwordHash = passwordHash,
            role = role,
            socialProvider = socialProvider,
            socialProviderId = socialProviderId,
        )
    }

    Given("UserRepository - save & findById") {
        When("유저를 저장하고 ID로 조회하면") {
            val user = createUser()
            val savedUser = userRepository.save(user)

            Then("저장된 유저의 ID가 생성된다") {
                savedUser.id shouldNotBe null
                savedUser.id shouldNotBe 0L
            }

            Then("ID로 조회할 수 있다") {
                val found = userRepository.findById(savedUser.requireId())
                found shouldNotBe null
                val result = requireNotNull(found)
                result.email shouldBe "test@example.com"
                result.name shouldBe "홍길동"
                result.role shouldBe UserRole.RENTER
            }
        }

        When("존재하지 않는 ID로 조회하면") {
            Then("null을 반환한다") {
                val found = userRepository.findById(999999L)
                found shouldBe null
            }
        }
    }

    Given("UserRepository - findByEmail") {
        When("이메일로 유저를 조회하면") {
            val user = createUser(email = "findbyemail@example.com")
            userRepository.save(user)

            Then("해당 유저가 반환된다") {
                val found = userRepository.findByEmail("findbyemail@example.com")
                found shouldNotBe null
                val result = requireNotNull(found)
                result.email shouldBe "findbyemail@example.com"
            }
        }

        When("존재하지 않는 이메일로 조회하면") {
            Then("null을 반환한다") {
                val found = userRepository.findByEmail("nonexistent@example.com")
                found shouldBe null
            }
        }
    }

    Given("UserRepository - findBySocialProviderAndSocialProviderId") {
        When("소셜 유저를 저장 후 소셜 정보로 조회하면") {
            val user = createUser(
                email = "social@example.com",
                socialProvider = SocialProvider.KAKAO,
                socialProviderId = "kakao_99999",
            )
            userRepository.save(user)

            Then("해당 소셜 유저가 반환된다") {
                val found = userRepository.findBySocialProviderAndSocialProviderId(
                    SocialProvider.KAKAO,
                    "kakao_99999",
                )
                found shouldNotBe null
                val result = requireNotNull(found)
                result.email shouldBe "social@example.com"
                result.socialProvider shouldBe SocialProvider.KAKAO
                result.socialProviderId shouldBe "kakao_99999"
            }
        }

        When("존재하지 않는 소셜 정보로 조회하면") {
            Then("null을 반환한다") {
                val found = userRepository.findBySocialProviderAndSocialProviderId(
                    SocialProvider.GOOGLE,
                    "google_nonexistent",
                )
                found shouldBe null
            }
        }
    }

    Given("UserRepository - existsByEmail") {
        When("존재하는 이메일을 확인하면") {
            val user = createUser(email = "exists@example.com")
            userRepository.save(user)

            Then("true를 반환한다") {
                val exists = userRepository.existsByEmail("exists@example.com")
                exists shouldBe true
            }
        }

        When("존재하지 않는 이메일을 확인하면") {
            Then("false를 반환한다") {
                val exists = userRepository.existsByEmail("notexists@example.com")
                exists shouldBe false
            }
        }
    }

    Given("UserRepository - 역할 전이 후 저장") {
        When("RENTER 유저가 LENDER 역할을 추가한 후 저장하면") {
            val user = createUser(email = "roletransition@example.com", role = UserRole.RENTER)
            val savedUser = userRepository.save(user)

            savedUser.addRole(UserRole.LENDER)
            val updatedUser = userRepository.save(savedUser)

            Then("역할이 BOTH로 저장된다") {
                val found = userRepository.findById(updatedUser.requireId())
                found shouldNotBe null
                val result = requireNotNull(found)
                result.role shouldBe UserRole.BOTH
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
