package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.AirCurrentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AirCurrentRepo : JpaRepository<AirCurrentEntity, Long> {
    fun findByStationId(stationId: Long): AirCurrentEntity?

    @Query("SELECT ac FROM AirCurrentEntity ac WHERE ac.stationId IN :stationIds")
    fun findAllByStationIdIn(stationIds: List<Long>): List<AirCurrentEntity>
}
