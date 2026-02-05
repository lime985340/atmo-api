package lime.atmo.application.port

import lime.atmo.domain.model.AirNow
import lime.atmo.domain.model.WeatherHour
import lime.atmo.domain.model.WeatherNow
import java.time.Instant
import java.util.UUID

data class LocationDto(
    val id: Long,
    val name: String,
    val stationId: Long?,
    val stationName: String?,
)

interface LocationPort {
    fun getLocation(locationId: Long): LocationDto?
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
    val name: String,
    val lat: Double,
    val lng: Double,
    val stationId: Long?,
    val stationName: String?,
)

interface FavoritePort {
    fun listFavorites(userId: UUID): List<FavoriteLocationDto>
    fun createFavorite(userId: UUID, name: String, lat: Double, lng: Double): FavoriteLocationDto
    fun removeFavorite(userId: UUID, favoriteId: Long)
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
