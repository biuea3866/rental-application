package com.rental.commerce.application.auth

import com.rental.commerce.domain.auth.AuthDomainService
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.SocialUserInfo
import com.rental.commerce.domain.common.TokenProvider
import com.rental.commerce.domain.common.SocialProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SocialLoginUseCaseTest : BehaviorSpec({

    val authDomainService = mockk<AuthDomainService>()
    val userDomainService = mockk<UserDomainService>()
    val tokenProvider = mockk<TokenProvider>()
    val refreshTokenService = mockk<RefreshTokenService>()
    val useCase = SocialLoginUseCase(
        authDomainService = authDomainService,
        userDomainService = userDomainService,
        tokenProvider = tokenProvider,
        refreshTokenService = refreshTokenService,
    )

    Given("소셜 로그인 시") {

        When("기존에 소셜 계정이 연결된 유저가 로그인하면") {
            val command = SocialLoginCommand(
                provider = SocialProvider.KAKAO,
                authorizationCode = "valid_auth_code",
            )

            val existingUser = User(
                email = "kakao@example.com",
                name = "카카오유저",
                phone = "01012345678",
                passwordHash = "SOCIAL_LOGIN",
                role = UserRole.RENTER,
                socialProvider = SocialProvider.KAKAO,
                socialProviderId = "kakao_123",
                id = 1L,
            )

            val socialUserInfo = SocialUserInfo(
                socialId = "kakao_123",
                email = "kakao@example.com",
                name = "카카오유저",
                provider = SocialProvider.KAKAO,
            )

            every { authDomainService.authenticateWithSocialProvider(SocialProvider.KAKAO, "valid_auth_code") } returns socialUserInfo
            every { userDomainService.findOrCreateSocialUser(socialUserInfo) } returns existingUser
            every { tokenProvider.createAccessToken(1L, "RENTER") } returns "access_token_abc"
            every { refreshTokenService.issueRefreshToken(1L) } returns RefreshTokenResult(
                refreshToken = "refresh_token_abc",
                tokenFamily = "family_abc",
                userId = 1L,
            )

            val result = useCase.execute(command)

            Then("액세스 토큰이 반환된다") {
                result.accessToken shouldBe "access_token_abc"
            }

            Then("리프레시 토큰이 반환된다") {
                result.refreshToken shouldBe "refresh_token_abc"
            }

            Then("유저 ID가 반환된다") {
                result.userId shouldBe 1L
            }
        }

        When("신규 유저가 카카오로 소셜 로그인하면") {
            val command = SocialLoginCommand(
                provider = SocialProvider.KAKAO,
                authorizationCode = "new_user_auth_code",
            )

            val newUser = User(
                email = "new_kakao@example.com",
                name = "신규카카오유저",
                phone = "",
                passwordHash = "SOCIAL_LOGIN",
                role = UserRole.RENTER,
                socialProvider = SocialProvider.KAKAO,
                socialProviderId = "kakao_new_456",
                id = 2L,
            )

            val socialUserInfo = SocialUserInfo(
                socialId = "kakao_new_456",
                email = "new_kakao@example.com",
                name = "신규카카오유저",
                provider = SocialProvider.KAKAO,
            )

            every { authDomainService.authenticateWithSocialProvider(SocialProvider.KAKAO, "new_user_auth_code") } returns socialUserInfo
            every { userDomainService.findOrCreateSocialUser(socialUserInfo) } returns newUser
            every { tokenProvider.createAccessToken(2L, "RENTER") } returns "access_token_new"
            every { refreshTokenService.issueRefreshToken(2L) } returns RefreshTokenResult(
                refreshToken = "refresh_token_new",
                tokenFamily = "family_new",
                userId = 2L,
            )

            val result = useCase.execute(command)

            Then("신규 유저가 생성되고 액세스 토큰이 반환된다") {
                result.accessToken shouldBe "access_token_new"
            }

            Then("신규 유저가 생성되고 리프레시 토큰이 반환된다") {
                result.refreshToken shouldBe "refresh_token_new"
            }

            Then("신규 유저가 생성되고 유저 ID가 반환된다") {
                result.userId shouldBe 2L
            }
        }

        When("같은 이메일로 이미 가입된 유저가 소셜 로그인하면") {
            val command = SocialLoginCommand(
                provider = SocialProvider.NAVER,
                authorizationCode = "existing_email_code",
            )

            val linkedUser = User(
                email = "existing@example.com",
                name = "기존유저",
                phone = "01012345678",
                passwordHash = "hashed_pw",
                role = UserRole.RENTER,
                socialProvider = SocialProvider.NAVER,
                socialProviderId = "naver_789",
                id = 3L,
            )

            val socialUserInfo = SocialUserInfo(
                socialId = "naver_789",
                email = "existing@example.com",
                name = "기존유저",
                provider = SocialProvider.NAVER,
            )

            every { authDomainService.authenticateWithSocialProvider(SocialProvider.NAVER, "existing_email_code") } returns socialUserInfo
            every { userDomainService.findOrCreateSocialUser(socialUserInfo) } returns linkedUser
            every { tokenProvider.createAccessToken(3L, "RENTER") } returns "access_token_linked"
            every { refreshTokenService.issueRefreshToken(3L) } returns RefreshTokenResult(
                refreshToken = "refresh_token_linked",
                tokenFamily = "family_linked",
                userId = 3L,
            )

            val result = useCase.execute(command)

            Then("소셜 계정이 연결되고 토큰이 반환된다") {
                result.accessToken shouldBe "access_token_linked"
                result.refreshToken shouldBe "refresh_token_linked"
                result.userId shouldBe 3L
            }
        }

        When("소셜 프로바이더에서 액세스 토큰 발급에 실패하면") {
            val command = SocialLoginCommand(
                provider = SocialProvider.KAKAO,
                authorizationCode = "invalid_code",
            )

            every {
                authDomainService.authenticateWithSocialProvider(SocialProvider.KAKAO, "invalid_code")
            } throws BusinessException(ErrorCode.EXTERNAL_API_ERROR, "카카오 액세스 토큰 발급 실패")

            Then("EXTERNAL_API_ERROR가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.EXTERNAL_API_ERROR
            }
        }
    }
})
