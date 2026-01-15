plugins { id("org.springframework.boot"); id("io.spring.dependency-management"); kotlin("jvm"); kotlin("plugin.spring") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies {
  implementation(project(":common"))
  implementation(project(":domain"))
  implementation(project(":application"))
  implementation(project(":infra"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  // Flyway 10 (Spring Boot 3.3) is modular: PostgreSQL support lives in a separate module.
  // Neon is PostgreSQL 17.x, so pin Flyway to a version that recognizes PG 17.
  implementation("org.flywaydb:flyway-core:10.20.1")
  runtimeOnly("org.flywaydb:flyway-database-postgresql:10.20.1")
  runtimeOnly("org.postgresql:postgresql")
  implementation("io.jsonwebtoken:jjwt-api:0.12.5")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
}
