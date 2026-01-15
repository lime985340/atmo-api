package lime.atmos.api

import lime.atmos.application.port.AirReadPort
import lime.atmos.application.port.LocationPort
import lime.atmos.application.port.WeatherReadPort
import lime.atmos.application.usecase.GetDashboardUseCase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// NOTE:
// - Our main class lives in package `lime.atmos.api`.
// - Infra adapters/repos/entities live under sibling packages (e.g. `lime.atmos.infra.*`).
// Spring Boot's default component/JPA scanning would miss those packages, which leads to
// missing beans like `UserPort` and missing JPA repositories.
@SpringBootApplication(scanBasePackages = ["lime.atmos"])
@EnableJpaRepositories(basePackages = ["lime.atmos.infra.jpa.repo"])
@EntityScan(basePackages = ["lime.atmos.infra.jpa.entity"])
class ApiApplication {

    @Bean
    fun getDashboardUseCase(
        locationPort: LocationPort,
        weatherReadPort: WeatherReadPort,
        airReadPort: AirReadPort
    ) = GetDashboardUseCase(locationPort, weatherReadPort, airReadPort)
}

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
