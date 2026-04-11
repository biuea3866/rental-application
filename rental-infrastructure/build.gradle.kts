plugins {
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    implementation(project(":rental-domain"))

    // JPA + QueryDSL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // MySQL
    runtimeOnly("com.mysql:mysql-connector-j")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // MinIO
    implementation("io.minio:minio:8.5.14")

    // Caffeine
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Security (PasswordEncoder)
    implementation("org.springframework.security:spring-security-crypto")

    // JWT (RS256)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Test
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:mysql:1.20.4")
    testImplementation("org.testcontainers:kafka:1.20.4")
    testImplementation("org.testcontainers:minio:1.20.4")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
