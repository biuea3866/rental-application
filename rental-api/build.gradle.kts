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

    testImplementation("org.springframework.security:spring-security-test")
}
