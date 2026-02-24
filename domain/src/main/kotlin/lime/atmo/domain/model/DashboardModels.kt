package lime.atmo.domain.model

import java.time.Instant

data class AdviceItem(
    val type: String,
    val level: String,
    val message: String,
)

data class Dashboard(
    val locationId: Long,
    val locationName: String,
    val weatherNow: WeatherNow?,
    val nextHours: List<WeatherHour>,
    val airNow: AirNow?,
    val advice: List<AdviceItem>,
    val dataUpdatedAt: Instant,
    val isStale: Boolean,
)
