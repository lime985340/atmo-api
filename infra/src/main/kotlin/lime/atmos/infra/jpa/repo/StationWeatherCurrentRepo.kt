package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.StationWeatherCurrentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface StationWeatherCurrentRepo : JpaRepository<StationWeatherCurrentEntity, Long> {
    fun findByStationId(stationId: Long?): StationWeatherCurrentEntity?

    @Query("SELECT swc FROM StationWeatherCurrentEntity swc WHERE swc.stationId IN :stationIds")
    fun findAllByStationIdIn(stationIds: List<Long>): List<StationWeatherCurrentEntity>
}
