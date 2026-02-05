package lime.atmos.application.usecase

import lime.atmos.application.port.AirReadPort
import lime.atmos.application.port.LocationPort
import lime.atmos.application.port.WeatherReadPort
import lime.atmos.domain.model.AdviceItem
import lime.atmos.domain.model.Dashboard
import java.time.Duration
import java.time.Instant

class GetDashboardUseCase(
    private val locationPort: LocationPort,
    private val weatherReadPort: WeatherReadPort,
    private val airReadPort: AirReadPort,
) {
    fun execute(locationId: Long): Dashboard {
        val loc = locationPort.getLocation(locationId)
            ?: throw IllegalArgumentException("location not found")

        val now = Instant.now()
        val weatherNow = loc.stationId?.let { weatherReadPort.getWeatherNow(it) }
        val nextHours = loc.stationId?.let { weatherReadPort.getNextHours(it, now, now.plus(Duration.ofHours(6))) } ?: emptyList()
        val airNow = loc.stationId?.let { airReadPort.getAirNow(it) }

        val updatedAt = listOfNotNull(
            loc.stationId?.let { weatherReadPort.getLatestUpdateTime(it) },
            loc.stationId?.let { airReadPort.getLatestUpdateTime(it) },
        ).maxOrNull() ?: now

        val isStale = Duration.between(updatedAt, now) > Duration.ofMinutes(30)

        val advice = buildList {
            val pm25 = airNow?.pm25
            if (pm25 != null && pm25 >= 35) {
                add(AdviceItem("AIR", "WARN", "미세먼지가 높아요. 장시간 외출은 피하세요."))
            }

            val precipType = weatherNow?.precipType
            if (precipType != null && precipType != "NONE") {
                add(AdviceItem("WEATHER", "INFO", "강수 가능성이 있어요. 우산을 챙기세요."))
            }
        }

        return Dashboard(
            loc.id,
            loc.name,
            weatherNow,
            nextHours,
            airNow,
            advice,
            updatedAt,
            isStale,
        )
    }
}
