package lime.atmo.application.usecase

import lime.atmo.application.port.AirReadPort
import lime.atmo.application.port.LocationPort
import lime.atmo.application.port.WeatherReadPort
import lime.atmo.domain.model.AdviceItem
import lime.atmo.domain.model.Dashboard
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

        // 실시간으로 가장 가까운 측정소 찾기
        val nearestStationId = locationPort.findNearestStationId(loc.lat, loc.lng)

        val weatherNow = nearestStationId?.let { weatherReadPort.getWeatherNow(it) }
        val nextHours = nearestStationId?.let { weatherReadPort.getNextHours(it, now, now.plus(Duration.ofHours(6))) } ?: emptyList()
        val airNow = nearestStationId?.let { airReadPort.getAirNow(it) }

        val updatedAt = listOfNotNull(
            nearestStationId?.let { weatherReadPort.getLatestUpdateTime(it) },
            nearestStationId?.let { airReadPort.getLatestUpdateTime(it) },
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
            loc.description,
            weatherNow,
            nextHours,
            airNow,
            advice,
            updatedAt,
            isStale,
        )
    }
}
