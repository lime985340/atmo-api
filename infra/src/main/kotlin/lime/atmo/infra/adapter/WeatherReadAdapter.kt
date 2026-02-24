package lime.atmo.infra.adapter

import lime.atmo.application.port.WeatherReadPort
import lime.atmo.domain.model.WeatherHour
import lime.atmo.domain.model.WeatherNow
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
import lime.atmo.infra.jpa.repo.WeatherForecastRepo
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 날씨 관련 Port 구현체
 * - 현재 날씨 조회
 * - 날씨 예보 조회
 */
@Component
class WeatherReadAdapter(
    private val stationWeatherRepo: StationWeatherCurrentRepo,
    private val forecastRepo: WeatherForecastRepo,
) : WeatherReadPort {

    override fun getWeatherNow(stationId: Long): WeatherNow? {
        return stationWeatherRepo.findByStationId(stationId)?.let {
            WeatherNow(
                observedAt = parseObs(it.obsTime),
                tempC = it.temp,
                humidity = it.humidity,
                windSpeed = it.windSpeed,
                precipType = null,
                precipMm = it.precip,
            )
        }
    }

    override fun getNextHours(stationId: Long, from: Instant, to: Instant): List<WeatherHour> {
        return forecastRepo.findByStationIdAndForecastAtBetweenOrderByForecastAt(stationId, from, to)
            .map {
                WeatherHour(
                    forecastAt = it.forecastAt,
                    tempC = it.temp?.toDouble(),
                    humidity = it.humidity?.toDouble(),
                    windSpeed = it.windSpeed?.toDouble(),
                    pop = it.pop?.toDouble(),
                    sky = it.sky,
                    precipType = it.precipType,
                    precipMm = it.precipMm?.toDouble(),
                )
            }
    }

    override fun getLatestUpdateTime(stationId: Long): Instant? {
        return stationWeatherRepo.findByStationId(stationId)?.updatedAt
    }

    /**
     * 관측 시간 문자열을 Instant로 변환
     * 예: "2024/01/15 14:00" -> Instant
     */
    private fun parseObs(raw: String): Instant {
        return try {
            val parts = raw.trim().split(" ")
            val date = parts.getOrNull(0) ?: return Instant.now()
            val time = parts.getOrNull(1) ?: "0000"
            val ldt = java.time.LocalDateTime.parse(
                date + time,
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm")
            )
            ldt.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant()
        } catch (e: Exception) {
            Instant.now()
        }
    }
}
