package com.rental.commerce.domain.user

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.UnauthorizedException
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

class UserTest : BehaviorSpec({

    fun createDefaultUser(
        email: String = "test@example.com",
        name: String = "홍길동",
        phone: String = "010-1234-5678",
        passwordHash: String = "hashed_password_123",
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

    Given("User 생성") {
        When("유효한 정보로 일반 유저를 생성하면") {
            val user = createDefaultUser()

            Then("유저 정보가 올바르게 설정된다") {
                user.email shouldBe "test@example.com"
                user.name shouldBe "홍길동"
                user.phone shouldBe "010-1234-5678"
                user.passwordHash shouldBe "hashed_password_123"
                user.role shouldBe UserRole.RENTER
            }

            Then("소셜 정보는 null이다") {
                user.socialProvider shouldBe null
                user.socialProviderId shouldBe null
            }
        }

        When("소셜 유저를 생성하면") {
            val user = createDefaultUser(
                socialProvider = SocialProvider.KAKAO,
                socialProviderId = "kakao_12345",
            )

            Then("소셜 정보가 올바르게 설정된다") {
                user.socialProvider shouldBe SocialProvider.KAKAO
                user.socialProviderId shouldBe "kakao_12345"
            }
        }
    }

    Given("addRole - 역할 전이") {
        When("RENTER 유저가 LENDER 역할을 추가하면") {
            val user = createDefaultUser(role = UserRole.RENTER)
            user.addRole(UserRole.LENDER)

            Then("역할이 BOTH로 전이된다") {
                user.role shouldBe UserRole.BOTH
            }
        }

        When("LENDER 유저가 RENTER 역할을 추가하면") {
            val user = createDefaultUser(role = UserRole.LENDER)
            user.addRole(UserRole.RENTER)

            Then("역할이 BOTH로 전이된다") {
                user.role shouldBe UserRole.BOTH
            }
        }

        When("BOTH 유저가 LENDER 역할을 추가하면") {
            val user = createDefaultUser(role = UserRole.BOTH)

            Then("예외 없이 BOTH를 유지한다") {
                shouldNotThrow<Exception> {
                    user.addRole(UserRole.LENDER)
                }
                user.role shouldBe UserRole.BOTH
            }
        }

        When("BOTH 유저가 RENTER 역할을 추가하면") {
            val user = createDefaultUser(role = UserRole.BOTH)

            Then("예외 없이 BOTH를 유지한다") {
                shouldNotThrow<Exception> {
                    user.addRole(UserRole.RENTER)
                }
                user.role shouldBe UserRole.BOTH
            }
        }

        When("RENTER 유저가 같은 RENTER 역할을 추가하면") {
            val user = createDefaultUser(role = UserRole.RENTER)

            Then("예외 없이 RENTER를 유지한다") {
                shouldNotThrow<Exception> {
                    user.addRole(UserRole.RENTER)
                }
                user.role shouldBe UserRole.RENTER
            }
        }

        When("LENDER 유저가 같은 LENDER 역할을 추가하면") {
            val user = createDefaultUser(role = UserRole.LENDER)

            Then("예외 없이 LENDER를 유지한다") {
                shouldNotThrow<Exception> {
                    user.addRole(UserRole.LENDER)
                }
                user.role shouldBe UserRole.LENDER
            }
        }

        When("RENTER 유저가 BOTH 역할을 추가하면") {
            val user = createDefaultUser(role = UserRole.RENTER)
            user.addRole(UserRole.BOTH)

            Then("역할이 BOTH로 전이된다") {
                user.role shouldBe UserRole.BOTH
            }
        }
    }

    Given("updateProfile") {
        When("이름과 전화번호를 변경하면") {
            val user = createDefaultUser()
            user.updateProfile(name = "김철수", phone = "010-9999-8888")

            Then("이름이 변경된다") {
                user.name shouldBe "김철수"
            }

            Then("전화번호가 변경된다") {
                user.phone shouldBe "010-9999-8888"
            }
        }

        When("이름만 변경하면") {
            val user = createDefaultUser()
            user.updateProfile(name = "김철수", phone = null)

            Then("이름만 변경되고 전화번호는 유지된다") {
                user.name shouldBe "김철수"
                user.phone shouldBe "010-1234-5678"
            }
        }

        When("전화번호만 변경하면") {
            val user = createDefaultUser()
            user.updateProfile(name = null, phone = "010-9999-8888")

            Then("전화번호만 변경되고 이름은 유지된다") {
                user.name shouldBe "홍길동"
                user.phone shouldBe "010-9999-8888"
            }
        }

        When("아무 값도 전달하지 않으면") {
            val user = createDefaultUser()
            user.updateProfile(name = null, phone = null)

            Then("아무것도 변경되지 않는다") {
                user.name shouldBe "홍길동"
                user.phone shouldBe "010-1234-5678"
            }
        }
    }

    Given("verifyPassword") {
        val passwordHasher = mockk<PasswordHasher>()

        When("올바른 비밀번호로 검증하면") {
            val user = createDefaultUser()
            every { passwordHasher.matches("correct_password", "hashed_password_123") } returns true

            Then("예외가 발생하지 않는다") {
                shouldNotThrow<Exception> {
                    user.verifyPassword("correct_password", passwordHasher)
                }
            }
        }

        When("잘못된 비밀번호로 검증하면") {
            val user = createDefaultUser()
            every { passwordHasher.matches("wrong_password", "hashed_password_123") } returns false

            Then("INVALID_PASSWORD 에러가 발생한다") {
                val exception = shouldThrow<UnauthorizedException> {
                    user.verifyPassword("wrong_password", passwordHasher)
                }
                exception.errorCode shouldBe ErrorCode.INVALID_PASSWORD
            }
        }
    }

    Given("isSocialUser") {
        When("소셜 정보가 있는 유저이면") {
            val user = createDefaultUser(
                socialProvider = SocialProvider.KAKAO,
                socialProviderId = "kakao_12345",
            )

            Then("true를 반환한다") {
                user.isSocialUser() shouldBe true
            }
        }

        When("소셜 정보가 없는 유저이면") {
            val user = createDefaultUser()

            Then("false를 반환한다") {
                user.isSocialUser() shouldBe false
            }
        }
    }
})
