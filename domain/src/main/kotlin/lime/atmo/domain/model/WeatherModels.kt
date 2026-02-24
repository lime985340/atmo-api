package lime.atmo.domain.model

import java.time.Instant

data class WeatherNow(
    val observedAt: Instant,
    val tempC: Double?,
    val humidity: Double?,
    val windSpeed: Double?,
    val precipType: String?,
    val precipMm: Double?,
)

data class WeatherHour(
    val forecastAt: Instant,
    val tempC: Double?,
    val humidity: Double?,
    val windSpeed: Double?,
    val pop: Double?,
    val sky: String?,
    val precipType: String?,
    val precipMm: Double?,
)
