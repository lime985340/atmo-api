package lime.atmo.infra.cache

import lime.atmo.common.util.GeoUtils
import lime.atmo.infra.jpa.entity.StationEntity
import lime.atmo.infra.jpa.repo.StationRepo
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Station 데이터 메모리 캐시 서비스
 * - 최초 사용 시 활성 Station 데이터를 메모리에 로드
 * - 정적 데이터 특성상 캐싱이 효율적
 */
@Service
class StationCacheService(
    private val stationRepo: StationRepo,
) {
    // ID 기반 캐시
    private val stationById = ConcurrentHashMap<Long, CachedStation>()

    // Grid(nx/ny) 기반 캐시 - 동일 그리드의 측정소 리스트
    private val stationsByGrid = ConcurrentHashMap<Pair<Int, Int>, List<CachedStation>>()

    // 전체 활성 Station 리스트 (좌표가 있는 것만)
    private var allActiveStations: List<CachedStation> = emptyList()
    @Volatile private var initialized = false

    data class CachedStation(
        val id: Long,
        val stationName: String,
        val addr: String?,
        val sido: String?,
        val lat: Double?,
        val lng: Double?,
        val nx: Int?,
        val ny: Int?,
    )

    /**
     * Avoid the extra eager station query during each API startup. The first caller that needs
     * nearest-station lookup loads this in-memory helper on demand.
     */
    fun initialize() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) refreshCache()
        }
    }

    /**
     * 캐시 새로고침 (Station 데이터 변경 시 호출)
     */
    @Synchronized
    fun refreshCache() {
        val stations = stationRepo.findAllActiveWithCoordinates()
            .map { it.toCachedStation() }

        // ID 기반 캐시
        stationById.clear()
        stationById.putAll(stations.associateBy { it.id })

        // 전체 리스트 캐시
        allActiveStations = stations

        // Grid 기반 캐시
        stationsByGrid.clear()
        stationsByGrid.putAll(
            stations
                .filter { it.nx != null && it.ny != null }
                .groupBy { Pair(it.nx!!, it.ny!!) },
        )
        initialized = true
    }

    /**
     * ID로 Station 조회
     */
    fun getById(id: Long): CachedStation? {
        initialize()
        return stationById[id]
    }

    /**
     * 모든 활성 Station 조회
     */
    fun getAllActive(): List<CachedStation> {
        initialize()
        return allActiveStations
    }

    /**
     * 특정 Grid(nx/ny)의 Station 리스트 조회
     */
    fun getByGrid(nx: Int, ny: Int): List<CachedStation> {
        initialize()
        return stationsByGrid[Pair(nx, ny)] ?: emptyList()
    }

    /**
     * 특정 위치에서 가장 가까운 Station 찾기 (메모리 기반)
     */
    fun findNearest(lat: Double, lng: Double): CachedStation? {
        return getAllActive()
            .filter { it.lat != null && it.lng != null }
            .minByOrNull { GeoUtils.haversineKm(lat, lng, it.lat!!, it.lng!!) }
    }

    /**
     * BBox 내에서 가장 가까운 Station 찾기 (메모리 기반)
     */
    fun findNearestInBBox(
        lat: Double,
        lng: Double,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
    ): CachedStation? {
        return getAllActive()
            .filter {
                it.lat != null && it.lng != null &&
                    it.lat in minLat..maxLat &&
                    it.lng in minLng..maxLng
            }
            .minByOrNull { GeoUtils.haversineKm(lat, lng, it.lat!!, it.lng!!) }
    }

    /**
     * Grid 기반으로 가장 가까운 Station 후보들 찾기
     * - 먼저 동일 그리드 검색
     * - 없으면 주변 그리드 검색
     */
    fun findNearestByGrid(lat: Double, lng: Double, nx: Int, ny: Int): CachedStation? {
        // 1. 동일 그리드에서 검색
        val sameGrid = getByGrid(nx, ny)
        if (sameGrid.isNotEmpty()) {
            return sameGrid
                .filter { it.lat != null && it.lng != null }
                .minByOrNull { GeoUtils.haversineKm(lat, lng, it.lat!!, it.lng!!) }
        }

        // 2. 주변 그리드 검색 (반경 1그리드)
        val nearbyStations = mutableListOf<CachedStation>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                nearbyStations.addAll(getByGrid(nx + dx, ny + dy))
            }
        }

        return nearbyStations
            .filter { it.lat != null && it.lng != null }
            .minByOrNull { GeoUtils.haversineKm(lat, lng, it.lat!!, it.lng!!) }
    }

    private fun StationEntity.toCachedStation(): CachedStation =
        CachedStation(
            id = this.id!!,
            stationName = this.stationName,
            addr = this.addr,
            sido = this.sido,
            lat = this.lat,
            lng = this.lng,
            nx = this.nx,
            ny = this.ny,
        )
}
