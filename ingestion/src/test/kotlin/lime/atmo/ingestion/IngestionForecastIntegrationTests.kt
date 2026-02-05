package lime.atmo.ingestion

import lime.atmo.infra.jpa.entity.StationEntity
import lime.atmo.infra.jpa.repo.StationRepo
import lime.atmo.infra.jpa.repo.WeatherForecastRepo
import lime.atmo.ingestion.kma.KmaFcstItem
import lime.atmo.ingestion.kma.KmaForecastClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test", "ingestion")
class IngestionForecastIntegrationTests {

    @Autowired
    private lateinit var stationRepo: StationRepo

    @Autowired
    private lateinit var forecastRepo: WeatherForecastRepo

    @Autowired
    private lateinit var runner: IngestionRunner

    @MockBean
    private lateinit var forecastClient: KmaForecastClient

    @Test
    fun `ingests forecast into repository`() {
        val station = stationRepo.save(
            StationEntity(
                provider = "AIRKOREA",
                stationName = "TEST",
                sido = "서울",
                lat = 37.0,
                lng = 127.0,
                nx = 60,
                ny = 127,
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        val items = listOf(
            KmaFcstItem("T1H", "20260204", "1200", "4"),
            KmaFcstItem("REH", "20260204", "1200", "65"),
            KmaFcstItem("WSD", "20260204", "1200", "2.1"),
            KmaFcstItem("POP", "20260204", "1200", "10"),
            KmaFcstItem("SKY", "20260204", "1200", "1"),
            KmaFcstItem("PTY", "20260204", "1200", "0"),
            KmaFcstItem("RN1", "20260204", "1200", "0"),
        )

        Mockito.`when`(
            forecastClient.fetchUltraSrtFcst(
                ArgumentMatchers.eq(60),
                ArgumentMatchers.eq(127),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
            ),
        ).thenReturn(items)

        runner.ingestForecast()

        val saved = forecastRepo.findByStationIdAndForecastAtBetweenOrderByForecastAt(
            station.id!!,
            Instant.parse("2026-02-04T00:00:00Z"),
            Instant.parse("2026-02-05T00:00:00Z"),
        )

        assertThat(saved).isNotEmpty
        assertThat(saved.first().temp).isEqualTo(4.0f)
    }
}
