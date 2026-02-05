package lime.atmo.ingestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
@SpringBootApplication(scanBasePackages = ["lime.atmo"])
@EnableJpaRepositories(basePackages = ["lime.atmo.infra.jpa.repo"])
@EntityScan(basePackages = ["lime.atmo.infra.jpa.entity"])
class IngestionApplication

fun main(args: Array<String>) {
    runApplication<IngestionApplication>(*args)
}
