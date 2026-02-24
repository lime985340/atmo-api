package lime.atmo.infra.adapter

import lime.atmo.application.port.FavoriteLocationDto
import lime.atmo.application.port.FavoritePort
import lime.atmo.infra.cache.GridStationMappingService
import lime.atmo.infra.cache.StationCacheService
import lime.atmo.infra.jpa.entity.FavoriteLocationEntity
import lime.atmo.infra.jpa.repo.FavoriteLocationRepository
import lime.atmo.infra.jpa.repo.StationRepo
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class FavoriteAdapter(
    private val favRepo: FavoriteLocationRepository,
    private val stationRepo: StationRepo,
    private val stationCacheService: StationCacheService,
    private val gridStationMappingService: GridStationMappingService,
) : FavoritePort {

    private fun FavoriteLocationEntity.toDto(): FavoriteLocationDto {
        return FavoriteLocationDto(
            id = this.id!!,
            description = this.description,
            lat = this.lat,
            lng = this.lon,
            address = this.address,
        )
    }

    override fun listFavorites(userId: UUID): List<FavoriteLocationDto> {
        val seen = mutableSetOf<String>()
        return favRepo.findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)
            .filter { favorite ->
                val dedupeKey = "grid:${favorite.nx}:${favorite.ny}"
                seen.add(dedupeKey)
            }
            .map { it.toDto() }
    }

    override fun createFavorite(userId: UUID, description: String, lat: Double, lng: Double, address: String?): FavoriteLocationDto {
        val (nx, ny) = gridStationMappingService.toGrid(lat, lng)

        val existing = favRepo.findFirstByUserIdAndIsActiveTrueAndNxAndNy(userId, nx, ny)
        if (existing != null) return existing.toDto()

        val trimmedDescription = description.trim()
        val resolvedDescription = if (trimmedDescription.isNotEmpty()) {
            trimmedDescription
        } else {
            "즐겨찾기"
        }

        val entity = FavoriteLocationEntity(
            userId = userId,
            description = resolvedDescription,
            lat = lat,
            lon = lng,
            nx = nx,
            ny = ny,
            address = address,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val saved = favRepo.save(entity)
        return saved.toDto()
    }

    override fun removeFavorite(userId: UUID, favoriteId: Long) {
        val updated = favRepo.softDelete(favoriteId, userId, Instant.now())
        if (updated == 0) throw IllegalArgumentException("favorite not found")
    }

    override fun findNearestStationId(lat: Double, lng: Double): Long? {
        val cachedStation = gridStationMappingService.findNearestStation(lat, lng)
        if (cachedStation != null) {
            return cachedStation.id
        }

        return stationRepo.findNearestWithPostGIS(lat, lng, maxDistanceKm = 100.0)?.id
    }
}
