dependencies {
    implementation(project(":rental-domain"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-crypto")
}
