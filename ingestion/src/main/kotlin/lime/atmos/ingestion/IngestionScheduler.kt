package lime.atmos.ingestion

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Profile("ingestion")
@Component
class IngestionScheduler(
    private val runner: IngestionRunner,
) {
    @Scheduled(cron = "0 10 3 ? * MON", zone = "Asia/Seoul")
    fun weeklyStationSeed() {
        runner.seedStationsIfMissing()
    }

    @Scheduled(cron = "0 10 * * * *", zone = "Asia/Seoul")
    fun hourlyIngestion() {
        runner.ingestAir()
        runner.ingestWeather()
        runner.ingestForecast()
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    fun dailyCleanup() {
        runner.cleanupForecasts()
    }
}
