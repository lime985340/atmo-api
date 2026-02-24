package lime.atmo.application.port

import java.util.UUID

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
