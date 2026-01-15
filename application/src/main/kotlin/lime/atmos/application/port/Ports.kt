package lime.atmos.application.port

import lime.atmos.domain.model.*
import java.time.Instant
import java.util.UUID

data class LocationDto(
    val id: Long,
    val name: String,
    val stationName: String
)

interface LocationPort {
    fun getLocation(locationId: Long): LocationDto?
}

interface WeatherReadPort {
    fun getWeatherNow(locationId: Long): WeatherNow?
    fun getNextHours(locationId: Long, from: Instant, to: Instant): List<WeatherHour>
    fun getLatestUpdateTime(locationId: Long): Instant?
}

interface AirReadPort {
    fun getAirNow(stationName: String): AirNow?
    fun getLatestUpdateTime(stationName: String): Instant?
}

interface FavoritePort {
    fun listFavorites(userId: UUID): List<Long>
    fun addFavorite(userId: UUID, locationId: Long)
    fun removeFavorite(userId: UUID, locationId: Long)
}

data class UserInfo(
    val id: UUID,
    val provider: String,
    val providerSubject: String,
    val email: String?,
    val name: String?
)

interface UserPort {
    fun findById(userId: UUID): UserInfo?
    
    fun findOrCreateByProvider(
        provider: String, 
        providerSubject: String, 
        email: String?, 
        name: String?
    ): UserInfo
}
