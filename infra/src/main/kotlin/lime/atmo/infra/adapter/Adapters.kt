package lime.atmo.infra.adapter

import lime.atmo.application.port.AirReadPort
import lime.atmo.application.port.FavoriteLocationDto
import lime.atmo.application.port.FavoritePort
import lime.atmo.application.port.LocationDto
import lime.atmo.application.port.LocationPort
import lime.atmo.application.port.UserInfo
import lime.atmo.application.port.UserPort
import lime.atmo.application.port.WeatherReadPort
import lime.atmo.domain.model.AirNow
import lime.atmo.domain.model.WeatherHour
import lime.atmo.domain.model.WeatherNow
import lime.atmo.infra.cache.GridStationMappingService
import lime.atmo.infra.cache.StationCacheService
import lime.atmo.infra.jpa.entity.AppUserEntity
import lime.atmo.infra.jpa.entity.FavoriteLocationEntity
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import lime.atmo.infra.jpa.repo.AppUserRepository
import lime.atmo.infra.jpa.repo.FavoriteLocationRepository
import lime.atmo.infra.jpa.repo.StationRepo
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
import lime.atmo.infra.jpa.repo.WeatherForecastRepo
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class LocationAdapter(
    private val repo: FavoriteLocationRepository,
    private val stationRepo: StationRepo,
    private val stationCacheService: StationCacheService,
    private val gridStationMappingService: GridStationMappingService,
) : LocationPort {
    override fun getLocation(locationId: Long): LocationDto? {
        return repo.findById(locationId).orElse(null)?.let {
            LocationDto(it.id!!, it.description, it.address, it.lat, it.lon)
        }
    }

    /**
     * 가장 가까운 측정소 ID 찾기 (최적화된 버전)
     * 1. Grid 기반 캐시 우선 검색 (O(1) ~ O(9))
     * 2. 메모리 내 BBox 검색
     * 3. PostGIS 공간 인덱스 검색 (fallback)
     */
    override fun findNearestStationId(lat: Double, lng: Double): Long? {
        // 1. Grid 기반 매핑 서비스로 먼저 검색 (캐시 활용)
        val cachedStation = gridStationMappingService.findNearestStation(lat, lng)
        if (cachedStation != null) {
            return cachedStation.id
        }

        // 2. PostGIS 기반 검색 (DB 레벨 최적화)
        return stationRepo.findNearestWithPostGIS(lat, lng, maxDistanceKm = 100.0)?.id
    }

    /**
     * Haversine 공식으로 두 지점 간 거리 계산 (km)
     */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0088
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

@Component
class WeatherReadAdapter(
    private val stationWeatherRepo: StationWeatherCurrentRepo,
    private val forecastRepo: WeatherForecastRepo,
) : WeatherReadPort {
    override fun getWeatherNow(stationId: Long): WeatherNow? {
        return stationWeatherRepo.findByStationId(stationId)?.let {
            WeatherNow(
                parseObs(it.obsTime),
                it.temp,
                it.humidity,
                it.windSpeed,
                null,
                it.precip,
            )
        }
    }

    override fun getNextHours(stationId: Long, from: Instant, to: Instant): List<WeatherHour> {
        return forecastRepo.findByStationIdAndForecastAtBetweenOrderByForecastAt(stationId, from, to)
            .map {
                WeatherHour(
                    forecastAt = it.forecastAt,
                    tempC = it.temp?.toDouble(),
                    humidity = it.humidity?.toDouble(),
                    windSpeed = it.windSpeed?.toDouble(),
                    pop = it.pop?.toDouble(),
                    sky = it.sky,
                    precipType = it.precipType,
                    precipMm = it.precipMm?.toDouble(),
                )
            }
    }

    override fun getLatestUpdateTime(stationId: Long): Instant? {
        return stationWeatherRepo.findByStationId(stationId)?.updatedAt
    }

    private fun parseObs(raw: String): Instant {
        return try {
            val parts = raw.trim().split(" ")
            val date = parts.getOrNull(0) ?: return Instant.now()
            val time = parts.getOrNull(1) ?: "0000"
            val ldt = java.time.LocalDateTime.parse(date + time, java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
            ldt.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant()
        } catch (e: Exception) {
            Instant.now()
        }
    }
}

@Component
class AirReadAdapter(
    private val repo: AirCurrentRepo,
) : AirReadPort {
    override fun getAirNow(stationId: Long): AirNow? {
        return repo.findByStationId(stationId)?.let {
            AirNow(
                it.dataTime,
                null,
                it.pm10?.toDouble(),
                it.pm25?.toDouble(),
                null,
                it.grade,
            )
        }
    }

    override fun getLatestUpdateTime(stationId: Long): Instant? {
        return repo.findByStationId(stationId)?.dataTime
    }
}

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

    /**
     * 가장 가까운 측정소 ID 찾기 (최적화된 버전)
     * 1. Grid 기반 캐시 우선 검색 (O(1) ~ O(9))
     * 2. 메모리 내 전체 검색
     * 3. PostGIS 공간 인덱스 검색 (fallback)
     */
    override fun findNearestStationId(lat: Double, lng: Double): Long? {
        // 1. Grid 기반 매핑 서비스로 먼저 검색 (캐시 활용)
        val cachedStation = gridStationMappingService.findNearestStation(lat, lng)
        if (cachedStation != null) {
            return cachedStation.id
        }

        // 2. PostGIS 기반 검색 (DB 레벨 최적화)
        return stationRepo.findNearestWithPostGIS(lat, lng, maxDistanceKm = 100.0)?.id
    }

    /**
     * Haversine 공식으로 두 지점 간 거리 계산 (km)
     */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0088
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

@Component
class UserAdapter(
    private val repo: AppUserRepository,
) : UserPort {
    override fun findById(userId: UUID): UserInfo? {
        return repo.findById(userId).orElse(null)?.let {
            UserInfo(it.id, it.provider, it.providerSubject, it.email, it.name)
        }
    }

    override fun findOrCreateByProvider(
        provider: String,
        providerSubject: String,
        email: String?,
        name: String?,
    ): UserInfo {
        val existing = repo.findByProviderAndProviderSubject(provider, providerSubject)
        val entity = if (existing != null) {
            existing.apply {
                this.email = email ?: this.email
                this.name = name ?: this.name
                this.updatedAt = Instant.now()
            }
        } else {
            AppUserEntity(
                id = UUID.randomUUID(),
                provider = provider,
                providerSubject = providerSubject,
                email = email,
                name = name,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        }
        val saved = repo.save(entity)
        return UserInfo(saved.id, saved.provider, saved.providerSubject, saved.email, saved.name)
    }
}
