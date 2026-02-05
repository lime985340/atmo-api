package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.StationEntity
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
}
