plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":rental-domain"))
    implementation(project(":rental-application"))
    implementation(project(":rental-infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:mysql:1.20.4")
    testImplementation("org.testcontainers:minio:1.20.4")
    testImplementation("io.minio:minio:8.5.14")
}
