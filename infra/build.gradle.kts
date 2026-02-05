plugins {
    // Apply Spring Boot plugin so this module receives Spring Boot's dependency BOM.
    // This is a library module, so we disable bootJar below.
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java-library")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":application"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation(kotlin("stdlib"))
}

// Library module: no executable boot jar here.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}
