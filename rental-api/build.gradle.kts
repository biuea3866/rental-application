plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":rental-domain"))
    implementation(project(":rental-application"))
    implementation(project(":rental-infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.data:spring-data-commons")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:mysql:1.20.4")
    testImplementation("org.testcontainers:minio:1.20.4")
    testImplementation("io.minio:minio:8.5.14")
    // BE-431 — 아키텍처 경계 검증 (NotificationPort 직접 주입 차단 등)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}
