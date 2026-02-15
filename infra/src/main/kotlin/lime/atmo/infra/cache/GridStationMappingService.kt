package lime.atmo.infra.cache

import lime.atmo.infra.cache.StationCacheService.CachedStation
import org.springframework.stereotype.Service

/**
 * Grid(nx/ny) 기반 Station 매핑 서비스
 * - 위도/경도를 그리드 좌표로 변환
 * - 그리드별 Station 매핑 캐싱
 */
@Service
class GridStationMappingService(
    private val stationCacheService: StationCacheService,
) {
    /**
     * 위도/경도를 기상청 격자 좌표(nx, ny)로 변환
     * (KMA DFS(LCC) grid conversion)
     */
    fun toGrid(lat: Double, lng: Double): Pair<Int, Int> {
        return GridConverter.toGrid(lat, lng)
    }

    /**
     * 특정 위치에서 가장 가까운 Station 찾기 (그리드 기반 우선)
     */
    fun findNearestStation(lat: Double, lng: Double): CachedStation? {
        val (nx, ny) = toGrid(lat, lng)

        // 1. 캐시된 그리드 기반 검색 (O(1) ~ O(9))
        val cachedResult = stationCacheService.findNearestByGrid(lat, lng, nx, ny)
        if (cachedResult != null) {
            return cachedResult
        }

        // 2. 캐시 미스 시 전체 검색
        return stationCacheService.findNearest(lat, lng)
    }

    /**
     * 특정 그리드의 Station 리스트 조회
     */
    fun getStationsByGrid(nx: Int, ny: Int): List<CachedStation> {
        return stationCacheService.getByGrid(nx, ny)
    }

    /**
     * 그리드 좌표가 유효한 범위 내에 있는지 확인
     */
    fun isValidGrid(nx: Int, ny: Int): Boolean {
        return nx in GRID_MIN_X..GRID_MAX_X && ny in GRID_MIN_Y..GRID_MAX_Y
    }

    companion object {
        // 기상청 격자좌표 범위
        const val GRID_MIN_X = 21
        const val GRID_MAX_X = 174
        const val GRID_MIN_Y = 33
        const val GRID_MAX_Y = 174
    }
}

/**
 * KMA DFS(LCC) grid conversion 유틸리티
 */
private object GridConverter {
    private const val RE = 6371.00877
    private const val GRID = 5.0
    private const val SLAT1 = 30.0
    private const val SLAT2 = 60.0
    private const val OLON = 126.0
    private const val OLAT = 38.0
    private const val XO = 43.0
    private const val YO = 136.0

    fun toGrid(lat: Double, lng: Double): Pair<Int, Int> {
        val degToRad = Math.PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * degToRad
        val slat2 = SLAT2 * degToRad
        val olon = OLON * degToRad
        val olat = OLAT * degToRad

        var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
        var ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
        ro = re * sf / Math.pow(ro, sn)

        var ra = Math.tan(Math.PI * 0.25 + lat * degToRad * 0.5)
        ra = re * sf / Math.pow(ra, sn)
        var theta = lng * degToRad - olon
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        val x = (ra * Math.sin(theta) + XO + 0.5).toInt()
        val y = (ro - ra * Math.cos(theta) + YO + 0.5).toInt()
        return Pair(x, y)
    }

    fun toLatLng(nx: Int, ny: Int): Pair<Double, Double> {
        val degToRad = Math.PI / 180.0
        val radToDeg = 180.0 / Math.PI
        val re = RE / GRID
        val slat1 = SLAT1 * degToRad
        val slat2 = SLAT2 * degToRad
        val olon = OLON * degToRad
        val olat = OLAT * degToRad

        var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
        var ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn)

        val x = nx.toDouble() - XO
        val y = ro - ny.toDouble() + YO

        var ra = Math.sqrt(x * x + y * y)
        if (sn < 0.0) ra = -ra

        var theta = 0.0
        if (Math.abs(x) <= 0.0) {
            theta = 0.0
        } else {
            theta = Math.atan2(x, y)
        }

        val lat = (Math.atan2(re * sf / ra, 1.0 / sn)).pow(1.0 / sn)
        val lon = theta / sn + olon

        return Pair(lat * radToDeg, lon * radToDeg)
    }

    private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
}
