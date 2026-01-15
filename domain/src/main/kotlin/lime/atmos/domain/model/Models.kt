package lime.atmos.domain.model

import java.time.Instant

data class WeatherNow(
    val observedAt: Instant,
    val tempC: Double?,
    val humidity: Double?,
    val windSpeed: Double?,
    val precipType: String?,
    val precipMm: Double?
)

data class WeatherHour(
    val forecastAt: Instant,
    val tempC: Double?,
    val humidity: Double?,
    val windSpeed: Double?,
    val pop: Double?,
    val sky: String?,
    val precipType: String?,
    val precipMm: Double?
)

data class AirNow(
    val measuredAt: Instant,
    val khai: Double?,
    val pm10: Double?,
    val pm25: Double?,
    val o3: Double?,
    val grade: String?
)

data class AdviceItem(
    val type: String,
    val level: String,
    val message: String
)

data class Dashboard(
    val locationId: Long,
    val locationName: String,
    val weatherNow: WeatherNow?,
    val nextHours: List<WeatherHour>,
    val airNow: AirNow?,
    val advice: List<AdviceItem>,
    val dataUpdatedAt: Instant,
    val isStale: Boolean
)
