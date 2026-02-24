package lime.atmo.infra.adapter

import lime.atmo.application.port.LocationDto
import lime.atmo.application.port.LocationPort
import lime.atmo.infra.cache.GridStationMappingService
import lime.atmo.infra.cache.StationCacheService
import lime.atmo.infra.jpa.repo.FavoriteLocationRepository
import lime.atmo.infra.jpa.repo.StationRepo
import org.springframework.stereotype.Component

/**
 * 위치 관련 Port 구현체
 * - 즐겨찾기 위치 조회
 * - 가장 가까운 측정소 찾기
 */
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
}
