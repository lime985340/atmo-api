package lime.atmo.domain.model

import java.time.Instant

data class AirNow(
    val measuredAt: Instant,
    val khai: Double?,
    val pm10: Double?,
    val pm25: Double?,
    val o3: Double?,
    val grade: Int?,
)
