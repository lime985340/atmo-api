package lime.atmo.application.port

import java.time.Instant
import java.util.UUID

data class CreateFavoriteRequest(
    val description: String,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
)

data class FavoriteDashboardCard(
    val id: Long,
    val description: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val weatherObsTime: String?,
    val tempC: Float?,
    val humidity: Float?,
    val windSpeed: Float?,
    val precipType: String?,
    val precipMm: Float?,
    val airDataTime: Instant?,
    val pm10: Float?,
    val pm25: Float?,
    val grade: Int?,
    val forecastHours: List<FavoriteForecastHour>,
    val stale: Boolean,
)

data class FavoriteForecastHour(
    val forecastAt: Instant,
    val tempC: Double?,
    val humidity: Double?,
    val windSpeed: Double?,
    val pop: Double?,
    val sky: String?,
    val precipType: String?,
    val precipMm: Double?,
)

interface FavoriteDashboardPort {
    fun getDashboard(userId: UUID): List<FavoriteDashboardCard>
}
