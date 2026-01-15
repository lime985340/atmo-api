package lime.atmos.ingestion

import lime.atmos.infra.jpa.entity.*
import lime.atmos.infra.jpa.repo.*
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Instant

@SpringBootApplication
class IngestionApplication(
    private val locationRepo: LocationRepository,
    private val weatherNowRepo: WeatherNowRepository,
    private val airNowRepo: AirNowRepository
) {
    @Bean
    fun runner() = CommandLineRunner { args ->
        val mode = args.firstOrNull { it.startsWith("--mode=") }?.substringAfter("=") ?: "ingest"
        println("[ingestion] mode=$mode")
        // TODO: 공공데이터 호출 + 정규화 + upsert
        // 지금은 upsert 패턴(샘플)만 제공
        if (locationRepo.count() == 0L) {
            locationRepo.save(
                LocationEntity(
                    name = "서울(샘플)",
                    lat = 37.5665,
                    lon = 126.9780,
                    nx = 60,
                    ny = 127,
                    stationName = "종로구"
                )
            )
        }
        val locId = locationRepo.findAll().first().id
        weatherNowRepo.save(
            WeatherNowEntity(
                id = WeatherNowId(locId, Instant.now()),
                tempC = 1.0,
                humidity = 40.0,
                windSpeed = 2.0,
                precipType = "NONE",
                precipMm = 0.0
            )
        )
        airNowRepo.save(
            AirNowEntity(
                id = AirNowId("종로구", Instant.now()),
                pm25 = 20.0,
                pm10 = 30.0,
                grade = "GOOD"
            )
        )
        println("[ingestion] done")
    }
}

fun main(args: Array<String>) {
    runApplication<IngestionApplication>(*args)
}
