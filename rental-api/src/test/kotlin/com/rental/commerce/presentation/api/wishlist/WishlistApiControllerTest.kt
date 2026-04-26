package com.rental.commerce.presentation.api.wishlist

import com.rental.commerce.application.wishlist.AddWishlistItemUseCase
import com.rental.commerce.application.wishlist.GetMyWishlistUseCase
import com.rental.commerce.application.wishlist.RemoveWishlistItemUseCase
import com.rental.commerce.application.wishlist.WishlistPageResult
import com.rental.commerce.application.wishlist.WishlistResult
import com.rental.commerce.domain.wishlist.WishlistAlreadyExistsException
import com.rental.commerce.domain.wishlist.WishlistNotFoundException
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper.Companion.HEADER_USER_ID
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZonedDateTime

class WishlistApiControllerTest : BehaviorSpec({

    val addWishlistItemUseCase = mockk<AddWishlistItemUseCase>()
    val removeWishlistItemUseCase = mockk<RemoveWishlistItemUseCase>()
    val getMyWishlistUseCase = mockk<GetMyWishlistUseCase>()

    val controller = WishlistApiController(
        addWishlistItemUseCase = addWishlistItemUseCase,
        removeWishlistItemUseCase = removeWishlistItemUseCase,
        getMyWishlistUseCase = getMyWishlistUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    val now = ZonedDateTime.now()
    val sampleWishlistResult = WishlistResult(
        id = 1L,
        userId = 42L,
        productId = 200L,
        createdAt = now,
    )

    beforeEach {
        clearMocks(addWishlistItemUseCase, removeWishlistItemUseCase, getMyWishlistUseCase)
    }

    Given("POST /api/v1/wishlist/{productId}") {

        When("위시리스트에 상품을 정상 추가하면") {
            Then("201 CREATED와 WishlistResult가 반환된다") {
                every { addWishlistItemUseCase.execute(userId = 42L, productId = 200L) } returns sampleWishlistResult

                val result = mockMvc.post("/api/v1/wishlist/200") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.id") { value(1) }
                    jsonPath("$.userId") { value(42) }
                    jsonPath("$.productId") { value(200) }
                }

                verify(exactly = 1) { addWishlistItemUseCase.execute(userId = 42L, productId = 200L) }
            }
        }

        When("이미 위시리스트에 담긴 상품을 추가하면") {
            Then("409 WISHLIST_ALREADY_EXISTS가 반환된다") {
                every {
                    addWishlistItemUseCase.execute(userId = 42L, productId = 200L)
                } throws WishlistAlreadyExistsException()

                val result = mockMvc.post("/api/v1/wishlist/200") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isConflict() }
                    jsonPath("$.code") { value("WISHLIST_ALREADY_EXISTS") }
                }
            }
        }

        When("X-Member-Id 헤더가 없으면") {
            Then("401 UNAUTHORIZED가 반환된다") {
                val result = mockMvc.post("/api/v1/wishlist/200")

                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }

    Given("DELETE /api/v1/wishlist/{productId}") {

        When("위시리스트에서 상품을 정상 삭제하면") {
            Then("204 NO_CONTENT가 반환된다") {
                justRun { removeWishlistItemUseCase.execute(userId = 42L, productId = 200L) }

                val result = mockMvc.delete("/api/v1/wishlist/200") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isNoContent() }
                }

                verify(exactly = 1) { removeWishlistItemUseCase.execute(userId = 42L, productId = 200L) }
            }
        }

        When("위시리스트에 없는 상품을 삭제하면") {
            Then("404 WISHLIST_NOT_FOUND가 반환된다") {
                every {
                    removeWishlistItemUseCase.execute(userId = 42L, productId = 999L)
                } throws WishlistNotFoundException()

                val result = mockMvc.delete("/api/v1/wishlist/999") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("WISHLIST_NOT_FOUND") }
                }
            }
        }
    }

    Given("GET /api/v1/wishlist") {

        When("위시리스트 목록을 정상 조회하면") {
            Then("200 OK와 페이지네이션된 위시리스트 목록이 반환된다") {
                val secondResult = WishlistResult(
                    id = 2L,
                    userId = 42L,
                    productId = 300L,
                    createdAt = now,
                )
                val pageResult = WishlistPageResult(
                    items = listOf(sampleWishlistResult, secondResult),
                    totalElements = 2L,
                    totalPages = 1,
                )
                every { getMyWishlistUseCase.execute(eq(42L), any()) } returns pageResult

                val result = mockMvc.get("/api/v1/wishlist") {
                    header(HEADER_USER_ID, "42")
                    param("page", "0")
                    param("size", "20")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.items.length()") { value(2) }
                    jsonPath("$.items[0].productId") { value(200) }
                    jsonPath("$.items[1].productId") { value(300) }
                    jsonPath("$.totalElements") { value(2) }
                    jsonPath("$.totalPages") { value(1) }
                }
            }
        }

        When("위시리스트가 비어 있으면") {
            Then("200 OK와 빈 목록이 반환된다") {
                val emptyPageResult = WishlistPageResult(
                    items = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )
                every { getMyWishlistUseCase.execute(eq(42L), any()) } returns emptyPageResult

                val result = mockMvc.get("/api/v1/wishlist") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.items.length()") { value(0) }
                    jsonPath("$.totalElements") { value(0) }
                }
            }
        }

        When("X-Member-Id 헤더가 없으면") {
            Then("401 UNAUTHORIZED가 반환된다") {
                val result = mockMvc.get("/api/v1/wishlist")

                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }
})
