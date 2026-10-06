package lime.atmo.infra.jpa.repo

import lime.atmo.infra.jpa.entity.WeatherObservationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface WeatherObservationRepo : JpaRepository<WeatherObservationEntity, Long> {
    fun findByStationIdAndObservedAt(stationId: Long, observedAt: Instant): WeatherObservationEntity?

    @Query("select w from WeatherObservationEntity w where w.stationId = :stationId order by w.observedAt desc limit 1")
    fun findLatest(@Param("stationId") stationId: Long): WeatherObservationEntity?
}
