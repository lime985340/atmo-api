plugins {
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    id("com.diffplug.spotless") version "6.22.0" apply false
}

allprojects {
    group = "lime.atmos"
    version = "0.2.0"
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            ktlint("0.50.0")
                .userData(mapOf("indent_size" to "4"))
            trimTrailingWhitespace()
            endWithNewline()
            indentWithSpaces(4)
        }

        kotlinGradle {
            target("**/*.kts")
            ktlint("0.50.0")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
