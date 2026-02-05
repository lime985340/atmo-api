package lime.atmo.api

import lime.atmo.application.port.AirReadPort
import lime.atmo.application.port.LocationPort
import lime.atmo.application.port.WeatherReadPort
import lime.atmo.application.usecase.GetDashboardUseCase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// NOTE:
// - Our main class lives in package `lime.atmo.api`.
// - Infra adapters/repos/entities live under sibling packages (e.g. `lime.atmo.infra.*`).
// Spring Boot's default component/JPA scanning would miss those packages, which leads to
// missing beans like `UserPort` and missing JPA repositories.
@SpringBootApplication(scanBasePackages = ["lime.atmo"])
@EnableCaching
@EnableJpaRepositories(basePackages = ["lime.atmo.infra.jpa.repo"])
@EntityScan(basePackages = ["lime.atmo.infra.jpa.entity"])
class ApiApplication {

    @Bean
    fun getDashboardUseCase(
        locationPort: LocationPort,
        weatherReadPort: WeatherReadPort,
        airReadPort: AirReadPort,
    ) = GetDashboardUseCase(locationPort, weatherReadPort, airReadPort)
}

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
