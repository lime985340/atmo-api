package lime.atmos.ingestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(scanBasePackages = ["lime.atmos"])
@EnableJpaRepositories(basePackages = ["lime.atmos.infra.jpa.repo"])
@EntityScan(basePackages = ["lime.atmos.infra.jpa.entity"])
class IngestionApplication

fun main(args: Array<String>) {
    runApplication<IngestionApplication>(*args)
}
