package lime.atmo.infra.jpa.repo

import lime.atmo.infra.jpa.entity.StationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StationRepo : JpaRepository<StationEntity, Long> {
    fun findByProviderAndStationNameAndSido(provider: String, stationName: String, sido: String?): StationEntity?

    @Query(
        """
        select s from StationEntity s
        where s.isActive = true
        and s.lat is not null and s.lng is not null
        and s.lat between :minLat and :maxLat
        and s.lng between :minLng and :maxLng
        """,
    )
    fun findActiveInBbox(
        @Param("minLat") minLat: Double,
        @Param("maxLat") maxLat: Double,
        @Param("minLng") minLng: Double,
        @Param("maxLng") maxLng: Double,
    ): List<StationEntity>

    // 카카오 지도 연동용 추가 메서드
    fun findAllByIsActiveTrue(): List<StationEntity>

    fun findAllByIsActiveTrueAndSido(sido: String): List<StationEntity>

    @Query("SELECT s FROM StationEntity s WHERE s.isActive = true AND s.lat IS NOT NULL AND s.lng IS NOT NULL")
    fun findAllActiveWithCoordinates(): List<StationEntity>

    // PostGIS 기반 가장 가까운 측정소 찾기 (네이티브 쿼리)
    @Query(
        """
        SELECT s.* FROM stations s
        WHERE s.is_active = true
          AND s.location IS NOT NULL
          AND ST_DWithin(
              s.location::geography,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
              :maxDistanceKm * 1000.0
          )
        ORDER BY ST_Distance(
            s.location::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        )
        LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findNearestWithPostGIS(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("maxDistanceKm") maxDistanceKm: Double = 50.0,
    ): StationEntity?

    // Grid(nx, ny)로 측정소 찾기
    fun findByNxAndNyAndIsActiveTrue(nx: Int, ny: Int): List<StationEntity>

    // 주변 Grid 범위로 측정소 찾기
    @Query(
        """
        SELECT s FROM StationEntity s
        WHERE s.isActive = true
          AND s.nx BETWEEN :minNx AND :maxNx
          AND s.ny BETWEEN :minNy AND :maxNy
          AND s.lat IS NOT NULL AND s.lng IS NOT NULL
        """,
    )
    fun findActiveInGridRange(
        @Param("minNx") minNx: Int,
        @Param("maxNx") maxNx: Int,
        @Param("minNy") minNy: Int,
        @Param("maxNy") maxNy: Int,
    ): List<StationEntity>
}
