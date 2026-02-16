package lime.atmo.application.port

import lime.atmo.domain.model.AirNow
import lime.atmo.domain.model.WeatherHour
import lime.atmo.domain.model.WeatherNow
import java.time.Instant
import java.util.UUID

data class LocationDto(
    val id: Long,
    val description: String,
    val address: String?,
    val lat: Double,
    val lng: Double,
)

interface LocationPort {
    fun getLocation(locationId: Long): LocationDto?
    fun findNearestStationId(lat: Double, lng: Double): Long?
}

interface WeatherReadPort {
    fun getWeatherNow(stationId: Long): WeatherNow?
    fun getNextHours(stationId: Long, from: Instant, to: Instant): List<WeatherHour>
    fun getLatestUpdateTime(stationId: Long): Instant?
}

interface AirReadPort {
    fun getAirNow(stationId: Long): AirNow?
    fun getLatestUpdateTime(stationId: Long): Instant?
}

data class FavoriteLocationDto(
    val id: Long,
    val description: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
)

interface FavoritePort {
    fun listFavorites(userId: UUID): List<FavoriteLocationDto>
    fun createFavorite(userId: UUID, description: String, lat: Double, lng: Double, address: String?): FavoriteLocationDto
    fun removeFavorite(userId: UUID, favoriteId: Long)
    fun findNearestStationId(lat: Double, lng: Double): Long?
}

data class UserInfo(
    val id: UUID,
    val provider: String,
    val providerSubject: String,
    val email: String?,
    val name: String?,
)

interface UserPort {
    fun findById(userId: UUID): UserInfo?

    fun findOrCreateByProvider(
        provider: String,
        providerSubject: String,
        email: String?,
        name: String?,
    ): UserInfo
}
