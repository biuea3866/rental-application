plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":rental-domain"))
    implementation(project(":rental-application"))
    implementation(project(":rental-infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-batch")
}
