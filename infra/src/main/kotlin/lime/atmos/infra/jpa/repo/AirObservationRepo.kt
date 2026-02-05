package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.AirObservationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AirObservationRepo : JpaRepository<AirObservationEntity, Long> {
    fun findByStationIdAndObservedAt(stationId: Long, observedAt: Instant): AirObservationEntity?

    @Query("select a from AirObservationEntity a where a.stationId = :stationId order by a.observedAt desc limit 1")
    fun findLatest(@Param("stationId") stationId: Long): AirObservationEntity?
}
