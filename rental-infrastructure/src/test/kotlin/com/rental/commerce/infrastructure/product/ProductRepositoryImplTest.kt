package com.rental.commerce.infrastructure.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductPrice
import com.rental.commerce.domain.product.ProductPriceRepository
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.RentalUnit
import com.rental.commerce.infrastructure.common.config.JpaAuditingConfig
import com.rental.commerce.infrastructure.common.config.QuerydslConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
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
        ProductRepositoryImpl::class,
        ProductPriceRepositoryImpl::class,
        ProductImageRepositoryImpl::class,
    ],
)
@ActiveProfiles("test")
class ProductRepositoryImplTest(
    private val productRepository: ProductRepository,
    private val productPriceRepository: ProductPriceRepository,
    private val productImageRepository: ProductImageRepository,
) : BehaviorSpec({

    extensions(SpringExtension)

    fun createProduct(
        userId: Long = 1L,
        name: String? = "테스트 상품",
        description: String? = "테스트 설명",
        categoryCode: String? = "ELECTRONICS",
        condition: ProductCondition? = ProductCondition.GOOD,
        depositAmount: Long? = 50_000L,
    ): Product {
        val product = Product(userId = userId)
        product.updateDraft(
            step = 1,
            name = name,
            description = description,
            categoryCode = categoryCode,
            condition = condition,
            depositAmount = depositAmount,
        )
        return product
    }

    // ===== ProductRepository =====

    Given("ProductRepository - save & findById") {
        When("상품을 저장하고 ID로 조회하면") {
            val product = createProduct()
            val savedProduct = productRepository.save(product)

            Then("저장된 상품의 ID가 생성된다") {
                savedProduct.productId shouldNotBe 0L
            }

            Then("ID로 조회할 수 있다") {
                val found = productRepository.findById(savedProduct.productId)
                found shouldNotBe null
                val result = requireNotNull(found)
                result.name shouldBe "테스트 상품"
                result.userId shouldBe 1L
            }
        }

        When("존재하지 않는 ID로 조회하면") {
            Then("null을 반환한다") {
                val found = productRepository.findById(999999L)
                found shouldBe null
            }
        }
    }

    Given("ProductRepository - findByUserId") {
        When("유저 ID로 상품을 조회하면") {
            val product1 = createProduct(userId = 100L, name = "상품1")
            val product2 = createProduct(userId = 100L, name = "상품2")
            val product3 = createProduct(userId = 200L, name = "상품3")
            productRepository.save(product1)
            productRepository.save(product2)
            productRepository.save(product3)

            Then("해당 유저의 상품 목록이 반환된다") {
                val products = productRepository.findByUserId(100L)
                products shouldHaveSize 2
            }
        }

        When("상품이 없는 유저 ID로 조회하면") {
            Then("빈 리스트를 반환한다") {
                val products = productRepository.findByUserId(999999L)
                products shouldHaveSize 0
            }
        }
    }

    // ===== ProductPriceRepository =====

    Given("ProductPriceRepository - saveAll & findByProductId") {
        When("상품 가격을 저장하고 상품 ID로 조회하면") {
            val product = productRepository.save(createProduct())
            val prices = listOf(
                ProductPrice(
                    productId = product.productId,
                    rentalUnit = RentalUnit.DAILY,
                    priceAmount = 10_000L,
                ),
                ProductPrice(
                    productId = product.productId,
                    rentalUnit = RentalUnit.MONTHLY,
                    priceAmount = 200_000L,
                ),
            )
            productPriceRepository.saveAll(prices)

            Then("저장된 가격 목록이 반환된다") {
                val found = productPriceRepository.findByProductId(product.productId)
                found shouldHaveSize 2
            }
        }

        When("가격이 없는 상품 ID로 조회하면") {
            Then("빈 리스트를 반환한다") {
                val found = productPriceRepository.findByProductId(999999L)
                found shouldHaveSize 0
            }
        }
    }

    Given("ProductPriceRepository - deleteByProductId") {
        When("상품 ID로 가격을 삭제하면") {
            val product = productRepository.save(createProduct())
            val prices = listOf(
                ProductPrice(
                    productId = product.productId,
                    rentalUnit = RentalUnit.DAILY,
                    priceAmount = 5_000L,
                ),
                ProductPrice(
                    productId = product.productId,
                    rentalUnit = RentalUnit.YEARLY,
                    priceAmount = 500_000L,
                ),
            )
            productPriceRepository.saveAll(prices)
            productPriceRepository.deleteByProductId(product.productId)

            Then("해당 상품의 가격이 모두 삭제된다") {
                val found = productPriceRepository.findByProductId(product.productId)
                found shouldHaveSize 0
            }
        }
    }

    // ===== ProductImageRepository =====

    Given("ProductImageRepository - saveAll & findByProductId") {
        When("상품 이미지를 저장하고 상품 ID로 조회하면") {
            val product = productRepository.save(createProduct())
            val images = listOf(
                ProductImage(
                    productId = product.productId,
                    objectKey = "products/${product.productId}/img-001.jpg",
                    originalFilename = "photo1.jpg",
                    sortOrder = 0,
                ),
                ProductImage(
                    productId = product.productId,
                    objectKey = "products/${product.productId}/img-002.jpg",
                    originalFilename = "photo2.jpg",
                    sortOrder = 1,
                ),
            )
            productImageRepository.saveAll(images)

            Then("정렬 순서대로 이미지 목록이 반환된다") {
                val found = productImageRepository.findByProductId(product.productId)
                found shouldHaveSize 2
                found[0].sortOrder shouldBe 0.toShort()
                found[1].sortOrder shouldBe 1.toShort()
            }
        }

        When("이미지가 없는 상품 ID로 조회하면") {
            Then("빈 리스트를 반환한다") {
                val found = productImageRepository.findByProductId(999999L)
                found shouldHaveSize 0
            }
        }
    }

    Given("ProductImageRepository - deleteByProductIdAndObjectKey") {
        When("상품 ID와 objectKey로 이미지를 삭제하면") {
            val product = productRepository.save(createProduct())
            val images = listOf(
                ProductImage(
                    productId = product.productId,
                    objectKey = "products/${product.productId}/delete-target.jpg",
                    originalFilename = "target.jpg",
                    sortOrder = 0,
                ),
                ProductImage(
                    productId = product.productId,
                    objectKey = "products/${product.productId}/keep.jpg",
                    originalFilename = "keep.jpg",
                    sortOrder = 1,
                ),
            )
            productImageRepository.saveAll(images)
            productImageRepository.deleteByProductIdAndObjectKey(
                product.productId,
                "products/${product.productId}/delete-target.jpg",
            )

            Then("해당 이미지만 삭제되고 나머지는 유지된다") {
                val found = productImageRepository.findByProductId(product.productId)
                found shouldHaveSize 1
                found[0].objectKey shouldBe "products/${product.productId}/keep.jpg"
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
